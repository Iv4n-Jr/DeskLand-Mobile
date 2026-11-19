package br.com.deskland.app.model

import kotlinx.serialization.Serializable

@Serializable
data class Produto(
    val id: Int,
    val nome: String,
    val categoria: String,
    val marca: String,
    val descricao: String,
    val centavos: Long,
    val estoque: Int
)

@Serializable
data class ItemPedido(
    val nome: String,
    val quantidade: Int,
    val subtotal: Long
)

@Serializable
data class Pedido(
    val data: Long,
    val total: Long,
    val quantidades: Map<Int, Int>,
    val itens: List<ItemPedido>
)
