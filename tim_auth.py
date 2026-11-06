"""Login Meu TIM com navegador isolado; segredos locais protegidos pelo Windows."""
import ctypes
import getpass
import json
import os
from pathlib import Path
from ctypes import wintypes

BASE = Path(__file__).resolve().parent
SESSION = BASE / '.tim-session.bin'
CREDENTIALS = BASE / '.tim-credentials.bin'


class Blob(ctypes.Structure):
    _fields_ = [('size', wintypes.DWORD), ('data', ctypes.POINTER(ctypes.c_ubyte))]


def protect(data, decrypt=False):
    if os.name != 'nt':
        raise RuntimeError('A proteção de segredos requer Windows.')
    buffer = ctypes.create_string_buffer(data)
    source = Blob(len(data), ctypes.cast(buffer, ctypes.POINTER(ctypes.c_ubyte)))
    destination = Blob()
    crypt = ctypes.windll.crypt32
    if decrypt:
        ok = crypt.CryptUnprotectData(ctypes.byref(source), None, None, None, None, 1, ctypes.byref(destination))
    else:
        ok = crypt.CryptProtectData(ctypes.byref(source), 'Meu TIM', None, None, None, 1, ctypes.byref(destination))
    if not ok:
        raise ctypes.WinError()
    try:
        return ctypes.string_at(destination.data, destination.size)
    finally:
        ctypes.windll.kernel32.LocalFree(destination.data)


def save_secret(path, data):
    temporary = path.with_suffix('.tmp')
    temporary.write_bytes(protect(json.dumps(data).encode()))
    os.replace(temporary, path)


def read_secret(path):
    return json.loads(protect(path.read_bytes(), decrypt=True))


def save_credentials(number, password):
    save_secret(CREDENTIALS, {'number': number, 'password': password})


def login():
    from playwright.sync_api import sync_playwright
    if not CREDENTIALS.exists():
        save_credentials(input('Número TIM com DDD: ').strip(), getpass.getpass('Senha Meu TIM: '))
    credentials = read_secret(CREDENTIALS)
    captured = {}
    with sync_playwright() as playwright:
        options = {'headless': True}
        if not Path(playwright.chromium.executable_path).exists():
            installed = sorted((Path(os.environ['LOCALAPPDATA']) / 'ms-playwright').glob('chromium-*/chrome-win64/chrome.exe'))
            if installed:
                options['executable_path'] = str(installed[-1])
        browser = playwright.chromium.launch(**options)
        try:
            context = browser.new_context()
            page = context.new_page()
            page.goto('https://meutim.tim.com.br/', wait_until='domcontentloaded', timeout=60000)
            page.locator('#campo-numero').press_sequentially(credentials['number'], delay=60)
            page.locator('#campo-senha').press_sequentially(credentials['password'], delay=60)
            page.locator('#campo-senha').press('Tab')
            # Campos e botão observados na página oficial de login.
            with page.expect_response(lambda response: '/api/v1/cards/card-internet/individual?scope=home' in response.url
                                      and response.status == 200, timeout=90000) as pending:
                page.locator('#btn-entrar').click()
            response = pending.value
            headers = response.request.all_headers()
            captured = {'headers': {name: value for name, value in headers.items()
                                   if not name.startswith(':') and name not in
                                   ('host', 'content-length', 'accept-encoding', 'connection')},
                        'storage': context.storage_state()}
            save_secret(SESSION, captured)
            print('Login concluído. Sessão salva com proteção do Windows.', flush=True)
        finally:
            browser.close()
    return captured['headers']


def headers(refresh=False):
    if not refresh and SESSION.exists():
        return read_secret(SESSION)['headers']
    return login()
