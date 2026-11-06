# Integração Meu TIM — descoberta em 2026-11-06

Consulta observada, autenticada na conta do usuário, com HTTP 200:

GET https://meutim-aks.timbrasil.com.br/api/v1/cards/card-internet/individual?scope=home
GET https://meutim-aks.timbrasil.com.br/api/v1/cards/card-internet/individual?scope=details

## Campos
- home: totalConsumption.internetPackage.totalCapacity.value (GB)
- home: totalConsumption.internetPackage.remaining.value (GB)
- Usado = total - disponível; percentual usado = usado / total * 100.
- details: internetPackages.renewDate = "30"; pacotes válidos até 30/11.
- details: internetPackages.packages[] contém nome, validade, total e saldo.
- details: footerTotalConsumption confirma 21,6 GB de 28 GB.

## Resultado observado
28 GB total; 21,6 GB disponíveis; aproximadamente 6,4 GB consumidos (22,86%).
Detalhes listam 20 GB de bônus (18,6 disponíveis) e 3 GB de internet (3 disponíveis).
A soma desses pacotes é 23 GB, inferior aos 28 GB do resumo: não reconstruir o total somando apenas os pacotes disponíveis. Investigar pacotes consumidos.
Não confundir vencimento da fatura com renovação da franquia.

## Autenticação e limitações
Requisições observadas incluem tid e stid e metadados channel=CAPTIVE, platform=WEB, environment=PRD, version=8.43.1, buildnumber=11.
O papel de tid/stid e o fluxo de renovação da sessão ainda não foram validados. Não considerar a integração automática pronta.
As capturas HAR permanecem locais, ignoradas pelo Git. Mesmo sanitizadas podem conter informações pessoais ou identificadores de sessão; não publicar.
Senha não foi salva em código nem neste documento.

## Pendências
- Obter início do ciclo ou histórico para média diária real.
- Rotas observadas: /consumption/individual/consumed e /consumption/individual/history.
- Validar autenticação fora do navegador, expiração e atualização da sessão.
- Definir se o widget mostra média desde o começo do ciclo ou média de observações coletadas.
