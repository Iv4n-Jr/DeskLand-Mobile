package br.com.deskland.app.data

import android.content.Context
import br.com.deskland.app.model.Pedido
import br.com.deskland.app.model.Produto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LojaRepository(context: Context) {
    private val assets = context.assets
    private val preferencias = context.getSharedPreferences("deskland", Context.MODE_PRIVATE)

    fun lerCatalogo(): List<Produto> =
        assets.open("catalogo.json").bufferedReader().use { Json.decodeFromString(it.readText()) }

    fun lerPedidos(): List<Pedido> =
        Json.decodeFromString(preferencias.getString("pedidos", "[]") ?: "[]")

    fun salvarPedidos(pedidos: List<Pedido>): Boolean =
        preferencias.edit().putString("pedidos", Json.encodeToString(pedidos)).commit()
}
