# Deskland Android com Jetpack Compose

Versão básica da loja de informática, escrita em Kotlin com Jetpack Compose e Material 3. O catálogo contém 34 produtos ativos em 12 categorias extraídas do banco fornecido.

## Abrir no Android Studio

1. Extraia o ZIP e abra a pasta `DesklandAndroid` em **File > Open**. Escolha a pasta com o arquivo `settings.gradle.kts`.
2. Aguarde o **Gradle Sync**. Se o Android Studio solicitar, instale o **Android SDK 35**. O projeto usa Gradle 8.9 e JDK 17. O primeiro sync precisa de internet.
3. Escolha um emulador ou celular com Android 6.0 ou superior e clique em **Run ▶**.

No Windows, também é possível gerar um APK de teste pelo PowerShell, na pasta do projeto, depois de configurar o SDK pelo Android Studio.

```powershell
.\gradlew.bat :app:assembleDebug
```

O APK será gerado em `app/build/outputs/apk/debug/app-debug.apk`.

## Testar o fluxo

1. Busque por `ryzen` e abra os detalhes de um produto.
2. Adicione o mesmo produto duas vezes. Abra o carrinho, retire uma unidade e confira o total.
3. Toque em **Simular pedido**. Confira a quantidade disponível no catálogo e abra **Pedidos** para ver o registro.
4. Feche e reabra o app. O pedido e o estoque atualizado devem continuar salvos no aparelho.

A simulação roda offline. O estoque parte do valor no catálogo e diminui conforme os pedidos confirmados naquele aparelho. Login, pagamento real e sincronização com a loja original estão fora do escopo desta versão.

## Onde editar

| Arquivo | Responsabilidade |
| --- | --- |
| `MainActivity.kt` | Inicia o Compose com `setContent`. |
| `ui/LojaTela.kt` | Catálogo, carrinho, detalhes, pedidos e tema em funções `@Composable`. Inclui uma prévia para o Android Studio. |
| `ui/LojaViewModel.kt` | Busca, carrinho, estoque e pedidos. Expõe o estado da tela via `StateFlow`. Usa `SavedStateHandle` para restaurar busca e carrinho após recriação do processo pelo Android. |
| `model/Modelos.kt` | Modelos `Produto`, `Pedido` e `ItemPedido` com `@Serializable`. |
| `data/LojaRepository.kt` | Usa Kotlin Serialization para ler o catálogo e salvar os pedidos no armazenamento local, no mesmo formato da versão anterior. |
| `app/src/main/assets/catalogo.json` | Catálogo local com preços em centavos e estoque inicial. |

A tela observa o ViewModel com `collectAsStateWithLifecycle`. Os botões chamam as funções do ViewModel, que atualiza o estado. O Compose redesenha as partes da tela que dependem desse estado.
