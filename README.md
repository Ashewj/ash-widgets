# ash-widgets

Aplicativo Android de widgets personaliz?veis. O primeiro card acompanha a franquia de internet do Meu TIM com consumo, saldo e indica??o da conex?o atual.

## Android

- Quatro abas: Home, Customiza??o, Outros e Login.
- Temas Opaque, Transparent, Opaque Smoky e Transparent Smoky, com seletor de cor.
- Consulta ? TIM diretamente pelo celular, com atualiza??o solicitada a cada hora. O Android pode adiar tarefas em segundo plano.
- Swipe vertical customizado nos previews do aplicativo: easeOutQuart, movimento de 120 ms e fade de 160 ms. O widget da tela inicial apresenta um ?nico card, sem swipe.
- Shader ao vivo no aplicativo; anima??o por frames no widget, sujeita ao agendamento do launcher.

O aplicativo solicita as credenciais durante o login e mant?m os segredos locais protegidos pelo Android Keystore. Nenhuma credencial, sess?o ou captura pessoal ? distribu?da neste reposit?rio. A integra??o utiliza interfaces do Meu TIM e pode exigir ajustes caso o servi?o mude.

### Build no Windows

Requer JDK 21, Android SDK Platform 35 e Build Tools 35.0.0. O script atual espera o SDK em `tools/sdk` e cont?m o caminho local do JDK; ajuste `$java` em `build-android.ps1` para sua instala??o.

```powershell
powershell -ExecutionPolicy Bypass -File build-android.ps1
```

Sa?da: `android/build/ash-widgets.apk`, assinado com uma chave local de desenvolvimento. SDK, chave e artefatos gerados s?o ignorados pelo Git.

Mais detalhes em [Android](android/README.md) e [Smoky e atribui??o do shader](android/SMOKY.md).

## Prot?tipo Python opcional

Ferramenta Windows independente para consultar o Meu TIM pelo computador:

```powershell
python -m pip install -r requirements.txt
python -m playwright install chromium
python tim_update.py
```

Atualiza a cada 60 segundos; `python tim_update.py --once` executa uma consulta. Solicita n?mero e senha no terminal. Usa Chromium isolado para autentica??o e reutiliza a sess?o, tentando novo login ap?s HTTP 401/403. Desafios adicionais da TIM podem exigir interven??o.

Credenciais e sess?o s?o criptografadas com DPAPI em `.tim-credentials.bin` e `.tim-session.bin`. O resultado local ? `tim-usage.json`. Esses arquivos e todas as capturas HAR ficam fora do Git.
