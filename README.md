# ash-widgets

Aplicativo Android de widgets personalizáveis. O primeiro card acompanha a franquia de internet do Meu TIM com consumo, saldo e indicação da conexão atual.

## Android

- Quatro abas: Home, Customização, Outros e Login.
- Temas Opaque, Transparent, Opaque Smoky e Transparent Smoky, com seletor de cor.
- Consulta à TIM diretamente pelo celular, com atualização solicitada a cada hora. O Android pode adiar tarefas em segundo plano.
- Swipe vertical customizado nos previews do aplicativo: easeOutQuart, movimento de 120 ms e fade de 160 ms. O widget da tela inicial apresenta um único card, sem swipe.
- Shader ao vivo no aplicativo; animação por frames no widget, sujeita ao agendamento do launcher.

O aplicativo solicita as credenciais durante o login e mantém os segredos locais protegidos pelo Android Keystore. Nenhuma credencial, sessão ou captura pessoal é distribuída neste repositório. A integração utiliza interfaces do Meu TIM e pode exigir ajustes caso o serviço mude.

### Build no Windows

Requer JDK 21, Android SDK Platform 35 e Build Tools 35.0.0. O script atual espera o SDK em `tools/sdk` e contém o caminho local do JDK; ajuste `$java` em `build-android.ps1` para sua instalação.

```powershell
powershell -ExecutionPolicy Bypass -File build-android.ps1
```

Saída: `android/build/ash-widgets.apk`, assinado com uma chave local de desenvolvimento. SDK, chave e artefatos gerados são ignorados pelo Git.

Mais detalhes em [Android](android/README.md) e [Smoky e atribuição do shader](android/SMOKY.md).

## Protótipo Python opcional

Ferramenta Windows independente para consultar o Meu TIM pelo computador:

```powershell
python -m pip install -r requirements.txt
python -m playwright install chromium
python tim_update.py
```

Atualiza a cada 60 segundos; `python tim_update.py --once` executa uma consulta. Solicita número e senha no terminal. Usa Chromium isolado para autenticação e reutiliza a sessão, tentando novo login após HTTP 401/403. Desafios adicionais da TIM podem exigir intervenção.

Credenciais e sessão são criptografadas com DPAPI em `.tim-credentials.bin` e `.tim-session.bin`. O resultado local é `tim-usage.json`. Esses arquivos e todas as capturas HAR ficam fora do Git.
