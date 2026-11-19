package br.com.deskland.app.ui

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import br.com.deskland.app.data.LojaRepository
import br.com.deskland.app.model.ItemPedido
import br.com.deskland.app.model.Pedido
import br.com.deskland.app.model.Produto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale

data class LojaEstado(
    val produtos: List<Produto> = emptyList(),
    val pedidos: List<Pedido> = emptyList(),
    val carrinho: Map<Int, Int> = emptyMap(),
    val busca: String = "",
    val salvando: Boolean = false,
    val mensagem: String? = null
) {
    val quantidadeCarrinho: Int get() = carrinho.values.sum()

    fun estoqueDisponivel(produto: Produto): Int =
        (produto.estoque - pedidos.sumOf { it.quantidades[produto.id] ?: 0 }).coerceAtLeast(0)

    fun produtosVisiveis(): List<Produto> {
        val filtro = normalizar(busca.trim())
        return produtos.filter {
            normalizar("${it.nome} ${it.marca} ${it.categoria}").contains(filtro)
        }
    }

    private fun normalizar(texto: String) =
        Normalizer.normalize(texto.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
}

class LojaViewModel(application: Application, private val salvo: SavedStateHandle) :
    AndroidViewModel(application) {
    private val repositorio = LojaRepository(application)
    private val carrinhoSalvo = salvo.get<Bundle>("carrinho")
    private val _estado = MutableStateFlow(
        LojaEstado(
            produtos = repositorio.lerCatalogo(),
            pedidos = repositorio.lerPedidos(),
            busca = salvo.get<String>("busca").orEmpty(),
            carrinho = carrinhoSalvo?.let { dados ->
                dados.keySet().associate { it.toInt() to dados.getInt(it) }
            }.orEmpty()
        )
    )
    val estado = _estado.asStateFlow()

    fun buscar(texto: String) {
        salvo["busca"] = texto
        _estado.update { it.copy(busca = texto) }
    }

    fun adicionar(produto: Produto) {
        val atual = _estado.value
        if (atual.salvando) return
        val quantidade = atual.carrinho[produto.id] ?: 0
        if (quantidade >= atual.estoqueDisponivel(produto)) {
            mostrarMensagem("Limite do estoque atingido.")
            return
        }
        atualizarCarrinho(atual.carrinho + (produto.id to quantidade + 1))
        mostrarMensagem("Adicionado ao carrinho.")
    }

    fun retirar(id: Int) {
        val atual = _estado.value
        if (atual.salvando) return
        val quantidade = atual.carrinho[id] ?: return
        val novo = if (quantidade == 1) atual.carrinho - id
        else atual.carrinho + (id to quantidade - 1)
        atualizarCarrinho(novo)
    }

    fun simularPedido() {
        val atual = _estado.value
        if (atual.salvando || atual.carrinho.isEmpty()) return
        val itens = mutableListOf<ItemPedido>()
        for ((id, quantidade) in atual.carrinho) {
            val produto = atual.produtos.find { it.id == id }
            if (produto == null || quantidade !in 1..atual.estoqueDisponivel(produto)) {
                mostrarMensagem("Confira a quantidade disponível no catálogo.")
                return
            }
            itens.add(ItemPedido(produto.nome, quantidade, produto.centavos * quantidade))
        }

        val pedido = Pedido(System.currentTimeMillis(), itens.sumOf { it.subtotal }, atual.carrinho, itens)
        val pedidos = atual.pedidos + pedido
        _estado.update { it.copy(salvando = true) }
        viewModelScope.launch {
            val gravou = withContext(Dispatchers.IO) { repositorio.salvarPedidos(pedidos) }
            if (gravou) {
                atualizarCarrinho(emptyMap())
                _estado.update { it.copy(pedidos = pedidos, salvando = false, mensagem = "Pedido simulado salvo.") }
            } else {
                _estado.update { it.copy(salvando = false, mensagem = "Não foi possível salvar o pedido.") }
            }
        }
    }

    fun limparMensagem() {
        _estado.update { it.copy(mensagem = null) }
    }

    private fun mostrarMensagem(texto: String) {
        _estado.update { it.copy(mensagem = texto) }
    }

    private fun atualizarCarrinho(carrinho: Map<Int, Int>) {
        salvo["carrinho"] = Bundle().apply {
            carrinho.forEach { (id, quantidade) -> putInt(id.toString(), quantidade) }
        }
        _estado.update { it.copy(carrinho = carrinho) }
    }
}
