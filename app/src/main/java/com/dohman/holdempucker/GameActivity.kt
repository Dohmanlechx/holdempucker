package com.dohman.holdempucker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.dohman.holdempucker.cards.Card
import kotlinx.android.synthetic.main.activity_game.*

class GameActivity : AppCompatActivity(), View.OnClickListener {
    lateinit var vm: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        vm = ViewModelProviders.of(this).get(GameViewModel::class.java)

        vm.pickedCardNotifier.observe(this, Observer { card_picked.setImageResource(it) })
        vm.cardsCountNotifier.observe(this, Observer { cards_left.text = it.toString() })

        vm.nfyCard.observe(this, Observer { updateCardImageResource(it) })
        vm.nfyBtmGoalie.observe(this, Observer { if (it) card_bm_goalie.setImageResource(R.drawable.red_back) }) // FIXME false = topGoalie

        setOnClickListeners()

    }

    private fun updateCardImageResource(value: Map<Array<Card?>, Int>) { // FIXME: Not needed?
        Log.d(TAG, value.toString())
    }

    private fun setOnClickListeners() {
        card_bm_forward_left.setOnClickListener(this)
        card_bm_center.setOnClickListener(this)
        card_bm_forward_right.setOnClickListener(this)
        card_bm_defender_left.setOnClickListener(this)
        card_bm_defender_right.setOnClickListener(this)
    }

    private fun updateCardImageView(view: AppCompatImageView) {
        view.setImageResource(vm.resIdOfCard(vm.currentCard))
    }

    override fun onClick(v: View) { // FIXME observe all cards?
        when (v.id) {
            R.id.card_bm_forward_left -> {
                if (card_bm_forward_left.drawable != null) return
                updateCardImageView(card_bm_forward_left)
                vm.setPlayerInTeam(teamBottom, 0)
            }
            R.id.card_bm_center -> {
                if (card_bm_center.drawable != null) return
                updateCardImageView(card_bm_center)
                vm.setPlayerInTeam(teamBottom, 1)
            }
            R.id.card_bm_forward_right -> {
                if (card_bm_forward_right.drawable != null) return
                updateCardImageView(card_bm_forward_right)
                vm.setPlayerInTeam(teamBottom, 2)
            }
            R.id.card_bm_defender_left -> {
                if (card_bm_defender_left.drawable != null) return
                updateCardImageView(card_bm_defender_left)
                vm.setPlayerInTeam(teamBottom, 3)
            }
            R.id.card_bm_defender_right -> {
                if (card_bm_defender_right.drawable != null) return
                updateCardImageView(card_bm_defender_right)
                vm.setPlayerInTeam(teamBottom, 4)
            }
        }
    }

    companion object {
        const val TAG = "GameActivity.kt"

        val teamTop = arrayOfNulls<Card>(6)
        val teamBottom = arrayOfNulls<Card>(6)

        /*  Index 0 = Left forward | 1 = Center | 2 = Right forward
                        3 = Left defender | 4 = Right defender
                                    5 = Goalie                          */
    }
}
