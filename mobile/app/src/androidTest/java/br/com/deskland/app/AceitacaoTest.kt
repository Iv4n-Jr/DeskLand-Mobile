
package br.com.deskland.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

class AceitacaoTest {

    @get:Rule
    val composeRule =
        createAndroidComposeRule<MainActivity>()

    @Test
    fun telaInicialDeveAparecer() {
        composeRule
            .onNodeWithTag("tela_inicial")
            .assertIsDisplayed()
    }

    
@Test
fun tituloDoAplicativoDeveAparecer() {
    composeRule
        .onNodeWithText("Deskland")
        .assertIsDisplayed()
}

@Test
fun botaoPedidosDeveAbrirJanela() {
    composeRule
        .onNodeWithText("Pedidos")
        .performClick()

    composeRule
        .onNodeWithText("Pedidos simulados")
        .assertIsDisplayed()
}

}
