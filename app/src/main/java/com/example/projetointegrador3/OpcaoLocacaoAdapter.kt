package com.example.projetointegrador3

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class OpcaoLocacaoAdapter(context: Context, private val opcoes: List<Pair<String, Boolean>>) :
    ArrayAdapter<Pair<String, Boolean>>(context, 0, opcoes) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_opcao_locacao, parent, false)

        val opcaoTextView: TextView = itemView.findViewById(R.id.opcao_text_view)
        val cadeadoImageView: ImageView = itemView.findViewById(R.id.cadeado_image_view)

        val (opcao, isDisponivel) = opcoes[position]

        opcaoTextView.text = opcao
        cadeadoImageView.visibility = if (isDisponivel) View.GONE else View.VISIBLE

        itemView.isEnabled = isDisponivel
        itemView.isClickable = isDisponivel

        return itemView
    }

    override fun isEnabled(position: Int): Boolean {
        return opcoes[position].second
    }

    override fun areAllItemsEnabled(): Boolean {
        return false
    }
}

