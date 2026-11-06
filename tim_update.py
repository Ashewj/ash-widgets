"""Consulta o Meu TIM a cada 60 segundos com login e renovação de sessão."""

import argparse
import json
import os
import sys
import time
from datetime import datetime, timezone
from decimal import Decimal
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import HTTPRedirectHandler, Request, build_opener

BASE = Path(__file__).resolve().parent
ENDPOINT = "https://meutim-aks.timbrasil.com.br/api/v1/cards/card-internet/individual"


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        # Evita encaminhar identificadores da sessão a outro destino.
        return None


def normalize(payload):
    package = payload["totalConsumption"]["internetPackage"]
    if any(package[key]["unit"].upper() != "GB" for key in ("totalCapacity", "remaining")):
        raise ValueError("Unidade inesperada; a resposta precisa estar em GB.")
    total = Decimal(package["totalCapacity"]["value"])
    remaining = Decimal(package["remaining"]["value"])
    if not total.is_finite() or not remaining.is_finite() or not 0 <= remaining <= total or total <= 0:
        raise ValueError("Franquia ou saldo inválido na resposta.")
    used = total - remaining
    return {
        "source": "Meu TIM",
        "capturedAt": datetime.now(timezone.utc).isoformat(),
        "totalGB": float(total),
        "remainingGB": float(remaining),
        "usedGB": float(used),
        "usedPercent": round(float(used / total * 100), 2),
        # O resumo não informa o início do ciclo; não inventar média ou renovação.
        "renewalDate": None,
        "averageGBPerDay": None,
    }


def update(output):
    from tim_auth import headers
    for attempt in range(2):
        session_headers = headers(refresh=attempt > 0)
        session_headers['accept-encoding'] = 'identity'
        request = Request(ENDPOINT + "?scope=home", headers=session_headers)
        try:
            with build_opener(NoRedirect).open(request, timeout=25) as response:
                payload = json.loads(response.read().decode("utf-8-sig"))
            break
        except HTTPError as error:
            if error.code not in (401, 403) or attempt:
                raise
            print('Sessão recusada. Refazendo o login...', flush=True)
    result = normalize(payload)
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_name(output.name + ".tmp")
    temporary.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    os.replace(temporary, output)
    print(
        f"[{datetime.now():%H:%M:%S}] "
        f"Usado: {result['usedGB']:g}/{result['totalGB']:g} GB "
        f"({result['usedPercent']:.2f}%) | "
        f"Disponível: {result['remainingGB']:g} GB",
        flush=True,
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=BASE / "tim-usage.json")
    parser.add_argument("--interval", type=float, default=60, help="Intervalo em segundos (mínimo 60).")
    parser.add_argument("--once", action="store_true", help="Faz uma consulta e encerra.")
    args = parser.parse_args()
    if args.interval < 60 or not args.interval < float("inf"):
        parser.error("Use um intervalo finito de pelo menos 60 segundos.")
    try:
        while True:
            started = time.monotonic()
            failed = False
            try:
                update(args.output)
            except HTTPError as error:
                failed = True
                if error.code in (401, 403):
                    message = "A TIM recusou a consulta mesmo após renovar o login."
                elif error.code == 429:
                    print("Limite da TIM atingido (HTTP 429). Encerrando; tente mais tarde.", file=sys.stderr)
                    return 1
                else:
                    message = "A TIM recusou a consulta."
                print(f"HTTP {error.code}: {message} Último JSON preservado.", file=sys.stderr, flush=True)
                if error.code in (401, 403):
                    return 1
            except (OSError, URLError, ValueError, KeyError, TypeError) as error:
                failed = True
                # Não imprimir respostas/headers: podem conter dados de sessão.
                print(f"Falha na consulta ({type(error).__name__}). Verifique HAR, conexão e formato da resposta. "
                      "Último JSON preservado.", file=sys.stderr, flush=True)
            if args.once:
                return int(failed)
            time.sleep(max(0, args.interval - (time.monotonic() - started)))
    except KeyboardInterrupt:
        print("\nAtualização encerrada.")
        return 0


if __name__ == "__main__":
    sys.exit(main())
