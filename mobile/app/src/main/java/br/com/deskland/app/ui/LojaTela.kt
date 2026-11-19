package br.com.deskland.app.ui

import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.deskland.app.model.Pedido
import br.com.deskland.app.model.Produto
import java.math.BigDecimal
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

private val ptBr = Locale.forLanguageTag("pt-BR")
private val cores = lightColorScheme(
    primary = Color(0xFF145548),
    background = Color(0xFFF4F6F3)
)

@Composable
fun DesklandTema(conteudo: @Composable () -> Unit) {
    MaterialTheme(colorScheme = cores, content = conteudo)
}

@Composable
fun LojaApp(modelo: LojaViewModel = viewModel()) {
    val estado by modelo.estado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    LaunchedEffect(estado.mensagem) {
        val mensagem = estado.mensagem ?: return@LaunchedEffect
        avisos.showSnackbar(mensagem)
        modelo.limparMensagem()
    }

    LojaTela(estado, avisos, modelo::buscar, modelo::adicionar, modelo::retirar, modelo::simularPedido)
}

@Composable
private fun LojaTela(
    estado: LojaEstado,
    avisos: SnackbarHostState,
    buscar: (String) -> Unit,
    adicionar: (Produto) -> Unit,
    retirar: (Int) -> Unit,
    simularPedido: () -> Unit
) {
    var janela by rememberSaveable { mutableStateOf("") }
    var produtoId by rememberSaveable { mutableStateOf(0) }
    val visiveis = remember(estado.produtos, estado.busca) { estado.produtosVisiveis() }
    val produto = estado.produtos.find { it.id == produtoId }
    val fechar = { janela = "" }

    Scaffold(
        snackbarHost = { SnackbarHost(avisos) },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { janela = "carrinho" }, modifier = Modifier.weight(1f)) {
                    Text("Carrinho (${estado.quantidadeCarrinho})")
                }
                Button(onClick = { janela = "pedidos" }) { Text("Pedidos") }
            }
        }
    ) { espaco ->
        
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(espaco)
        .padding(horizontal = 16.dp)
        .testTag("tela_inicial"),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    Text(
        "Deskland",
        style = MaterialTheme.typography.headlineLarge,
        color = cores.primary
    )
            Text("Catálogo de informática")
            OutlinedTextField(
                value = estado.busca,
                onValueChange = buscar,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar nome, marca ou categoria") },
                singleLine = true
            )
            if (visiveis.isEmpty()) Text("Nenhum produto encontrado.")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visiveis, key = { it.id }) { item ->
                    ProdutoCard(
                        item,
                        estado.estoqueDisponivel(item),
                        estado.salvando,
                        detalhes = {
                            produtoId = item.id
                            janela = "produto"
                        },
                        adicionar = { adicionar(item) }
                    )
                }
            }
        }
    }

    when (janela) {
        "produto" -> if (produto != null) AlertDialog(
            onDismissRequest = fechar,
            title = { Text(produto.nome) },
            text = { Text("${produto.marca}\n\n${produto.descricao.ifBlank { "Descrição indisponível." }}") },
            confirmButton = { TextButton(onClick = fechar) { Text("OK") } }
        )
        "carrinho" -> CarrinhoDialog(estado, fechar, retirar, simularPedido)
        "pedidos" -> PedidosDialog(estado.pedidos, fechar)
    }
}

@Composable
private fun ProdutoCard(
    produto: Produto,
    estoque: Int,
    salvando: Boolean,
    detalhes: () -> Unit,
    adicionar: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(produto.categoria, style = MaterialTheme.typography.labelMedium)
            Text(produto.nome, style = MaterialTheme.typography.titleMedium)
            Text(preco(produto.centavos), color = cores.primary, style = MaterialTheme.typography.titleMedium)
            Text(if (estoque > 0) "Em estoque: $estoque" else "Esgotado")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = detalhes) { Text("Detalhes") }
                Button(onClick = adicionar, enabled = estoque > 0 && !salvando) { Text("Adicionar") }
            }
        }
    }
}

@Composable
private fun CarrinhoDialog(
    estado: LojaEstado,
    fechar: () -> Unit,
    retirar: (Int) -> Unit,
    simularPedido: () -> Unit
) {
    val itens = estado.produtos.filter { it.id in estado.carrinho }
    val total = itens.sumOf { it.centavos * estado.carrinho.getValue(it.id) }
    AlertDialog(
        onDismissRequest = fechar,
        title = { Text("Carrinho") },
        text = {
            if (itens.isEmpty()) {
                Text("Seu carrinho está vazio.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyColumn(
                        Modifier.heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(itens, key = { it.id }) { produto ->
                            val quantidade = estado.carrinho.getValue(produto.id)
                            Column {
                                Text(produto.nome, style = MaterialTheme.typography.titleSmall)
                                Text("$quantidade un. · ${preco(produto.centavos * quantidade)}")
                                TextButton(onClick = { retirar(produto.id) }, enabled = !estado.salvando) {
                                    Text("Retirar 1")
                                }
                            }
                        }
                    }
                    Text("Total: ${preco(total)}", style = MaterialTheme.typography.titleMedium)
                    if (estado.salvando) Text("Salvando pedido…")
                }
            }
        },
        confirmButton = {
            if (itens.isNotEmpty()) {
                TextButton(onClick = {
                    simularPedido()
                    fechar()
                }, enabled = !estado.salvando) {
                    Text("Simular pedido")
                }
            }
        },
        dismissButton = { TextButton(onClick = fechar) { Text("Voltar") } }
    )
}

@Composable
private fun PedidosDialog(pedidos: List<Pedido>, fechar: () -> Unit) {
    AlertDialog(
        onDismissRequest = fechar,
        title = { Text("Pedidos simulados") },
        text = {
            if (pedidos.isEmpty()) {
                Text("Você ainda não tem pedidos.")
            } else {
                LazyColumn(
                    Modifier.heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pedidos.size) { indice ->
                        val numero = pedidos.size - indice
                        val pedido = pedidos[numero - 1]
                        val data = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, ptBr)
                            .format(Date(pedido.data))
                        Column {
                            Text("Pedido #$numero · $data", style = MaterialTheme.typography.titleSmall)
                            for (item in pedido.itens) Text("${item.quantidade} × ${item.nome}")
                            Text("Total: ${preco(pedido.total)}")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = fechar) { Text("OK") } }
    )
}

private fun preco(centavos: Long) =
    NumberFormat.getCurrencyInstance(ptBr).format(BigDecimal.valueOf(centavos, 2))

@Preview(showBackground = true)
@Composable
private fun CatalogoPreview() {
    val exemplo = Produto(1, "Processador AMD Ryzen 5 5600", "Processadores", "AMD", "", 69990, 17)
    DesklandTema {
        LojaTela(LojaEstado(produtos = listOf(exemplo)), remember { SnackbarHostState() }, {}, {}, {}, {})
    }
}
