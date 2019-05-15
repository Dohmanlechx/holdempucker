package com.dohman.holdempucker.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.dohman.holdempucker.R
import com.dohman.holdempucker.cards.Card
import com.dohman.holdempucker.ui.MessageTextItem
import com.dohman.holdempucker.util.Constants
import com.dohman.holdempucker.util.Constants.Companion.isOngoingGame
import com.dohman.holdempucker.util.Constants.Companion.isJustShotAtGoalie
import com.dohman.holdempucker.util.Constants.Companion.period
import com.dohman.holdempucker.util.Constants.Companion.possibleMovesIndexes
import com.dohman.holdempucker.util.Constants.Companion.isRestoringPlayers
import com.dohman.holdempucker.util.Constants.Companion.teamBottom
import com.dohman.holdempucker.util.Constants.Companion.teamBottomScore
import com.dohman.holdempucker.util.Constants.Companion.teamTop
import com.dohman.holdempucker.util.Constants.Companion.teamTopScore
import com.dohman.holdempucker.util.Constants.Companion.whoseTeamStartedLastPeriod
import com.dohman.holdempucker.util.Constants.Companion.whoseTurn
import com.dohman.holdempucker.util.GameLogic
import com.dohman.holdempucker.util.Animations
import com.dohman.holdempucker.util.Constants.Companion.TAG_GAMEACTIVITY
import com.dohman.holdempucker.util.Constants.Companion.isBotMoving
import com.dohman.holdempucker.util.Constants.WhoseTurn.Companion.isTeamBottomTurn
import com.dohman.holdempucker.util.ViewUtil
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.wajahatkarim3.easyflipview.EasyFlipView
import kotlinx.android.synthetic.main.computer_layout.*
import kotlinx.android.synthetic.main.game_fragment.*

class GameFragment : Fragment(), View.OnClickListener {
    private lateinit var vm: GameViewModel

    private val itemAdapter = ItemAdapter<AbstractItem<*, *>>()
    private val fastAdapter = FastAdapter.with<AbstractItem<*, *>, ItemAdapter<AbstractItem<*, *>>>(itemAdapter)

    // x and y values of all three FlipViews
    private var fvMainX: Float = 0f
    private var fvMainY: Float = 0f
    private var fvGoalieBtmX: Float = 0f
    private var fvGoalieBtmY: Float = 0f
    private var fvGoalieTopX: Float = 0f
    private var fvGoalieTopY: Float = 0f

    private val teamBottomViews = mutableListOf<AppCompatImageView>()
    private val teamTopViews = mutableListOf<AppCompatImageView>()

    private var tempGoalieCard: Card? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        vm = ViewModelProviders.of(this).get(GameViewModel::class.java)

        // Observables
        vm.messageNotifier.observe(this, Observer { updateMessageBox(it.first, it.second) })
        vm.halfTimeNotifier.observe(this, Observer {
            if (isNextPeriodReady(it)) addGoalieView(true, withStartDelay = true)
        })
        vm.whoseTurnNotifier.observe(this, Observer { Animations.animatePuck(puck, it) })
        vm.pickedCardNotifier.observe(this, Observer { flipNewCard(it) })
        vm.cardsCountNotifier.observe(this, Observer { cards_left.text = it.toString() })
        vm.badCardNotifier.observe(
            this,
            Observer {
                flipNewCard(vm.resIdOfCard(vm.firstCardInDeck), isBadCard = true)
                vm.notifyMessage("Aw, too weak card! It goes out!")
            })
        // End of Observables

        return inflater.inflate(R.layout.game_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        flip_view.post {
            flip_view.bringToFront()
            fvMainX = flip_view.x
            fvMainY = flip_view.y
        }

        flip_btm_goalie.post {
            ViewUtil.setScaleOnRotatedView(flip_view, card_bm_goalie)
            ViewUtil.setScaleOnRotatedView(flip_view, background_bm_goalie)
            ViewUtil.setScaleOnRotatedView(flip_view, flip_btm_goalie)
        }

        flip_top_goalie.post {
            ViewUtil.setScaleOnRotatedView(flip_view, card_top_goalie)
            ViewUtil.setScaleOnRotatedView(flip_view, background_top_goalie)
            ViewUtil.setScaleOnRotatedView(flip_view, flip_top_goalie)
        }

        computer_lamp.post {
            Animations.animateLamp(computer_lamp)
        }

        setupMessageRecycler()
        updateMessageBox("Press anywhere to start the game! Period: $period", isNeutralMessage = true)

        whole_view.setOnClickListener { initGame() }
    }

    override fun onResume() {
        super.onResume()
        vm.setGameMode()
        storeAllViews()
        setOnClickListeners()
    }

    private fun initGame() {
        period = 1

        teamBottomScore = 0
        teamTopScore = 0
        updateScores()

        Constants.resetBooleansToInitState()

        resetAllCards(teamBottomViews)
        resetAllCards(teamTopViews)
        card_top_goalie.tag = null
        card_bm_goalie.tag = null

        addGoalieView(true)
        whole_view.visibility = View.GONE
    }

    /*
    * Views management
    * */

    private fun updateScores() {
        top_team_score.text = teamTopScore.toString()
        bm_team_score.text = teamBottomScore.toString()
    }

    private fun restoreFlipViewsPosition() {
        flip_view.rotation = 0f
        flip_view.x = fvMainX
        flip_view.y = fvMainY
        flip_btm_goalie.x = fvGoalieBtmX
        flip_btm_goalie.y = fvGoalieBtmY
        flip_top_goalie.x = fvGoalieTopX
        flip_top_goalie.y = fvGoalieTopY
    }

    private fun storeAllViews() {
        teamBottomViews.clear()
        teamTopViews.clear()

        teamBottomViews.apply {
            add(card_bm_forward_left)
            add(card_bm_center)
            add(card_bm_forward_right)
            add(card_bm_defender_left)
            add(card_bm_defender_right)
            add(card_bm_goalie)
        }

        teamTopViews.apply {
            add(card_top_forward_left)
            add(card_top_center)
            add(card_top_forward_right)
            add(card_top_defender_left)
            add(card_top_defender_right)
            add(card_top_goalie)
        }
    }

    private fun resetAllCards(cardImageViews: List<AppCompatImageView>) {
        cardImageViews.forEach {
            it.setImageResource(android.R.color.transparent)
            it.tag = Integer.valueOf(android.R.color.transparent)
        }
    }

    private fun setupMessageRecycler() = v_recycler.apply {
        itemAnimator = null
        layoutManager = LinearLayoutManager(requireContext())
        adapter = fastAdapter
    }

    private fun updateMessageBox(message: String, isNeutralMessage: Boolean = false) {
        itemAdapter.clear()
        itemAdapter.add(MessageTextItem(message, isNeutralMessage))
    }

    /*
    * Animation initializer
    * */

    private fun flipNewCard(resId: Int, isBadCard: Boolean = false) {
        ViewUtil.setImagesOnFlipView(flip_view, card_deck, card_picked, resId, null, isVertical = true)

        Animations.animateFlipPlayingCard(
            flip_view,
            cards_left,
            vm.cardDeck.size > 50,
            { onFlipPlayingCardEnd(isBadCard) },
            { message -> vm.notifyMessage(message) })
    }

    private fun prepareViewsToPulse() {
        val teamToPulse = if (isTeamBottomTurn()) teamTopViews else teamBottomViews
        val viewsToPulse = mutableListOf<AppCompatImageView>()

        possibleMovesIndexes.forEach {
            viewsToPulse.add(teamToPulse[it])
        }

        Animations.animatePulsingCards(viewsToPulse as List<AppCompatImageView>) { message ->
            updateMessageBox(message)
        }
    }

    private fun addGoalieView(
        bottom: Boolean,
        doNotFlip: Boolean = false,
        doRemoveCardFromDeck: Boolean = false,
        withStartDelay: Boolean = false
    ) {
        if (fvGoalieBtmX == 0f) {
            fvGoalieBtmX = flip_btm_goalie.x
            fvGoalieBtmY = flip_btm_goalie.y
            fvGoalieTopX = flip_top_goalie.x
            fvGoalieTopY = flip_top_goalie.y
        }

        // ONLY adding view. No real goalie card is assigning to that team by this function.
        removeAllOnClickListeners()

        val view = if (bottom) card_bm_goalie else card_top_goalie

        card_deck.setImageResource(R.drawable.red_back_vertical)
        card_picked.setImageResource(R.drawable.red_back_vertical)

        val delay: Long = if (withStartDelay) 1500 else 150

        Animations.animateAddGoalie(
            flipView = flip_view,
            goalie = view,
            xForAttacker = card_bm_center.x,
            delay = delay
        )
        {
            // onStop
            restoreFlipViewsPosition()
            vm.onGoalieAddedAnimationEnd(view)
            if (card_top_goalie.tag != Integer.valueOf(R.drawable.red_back)) addGoalieView(bottom = false) else {
                if (!doNotFlip) flipNewCard(vm.resIdOfCard(vm.firstCardInDeck)) // FIXME
                if (doRemoveCardFromDeck) vm.removeCardFromDeck(doNotNotify = true)
                vm.gameManager()
                cards_left.visibility = View.VISIBLE
            }
        }
    }

    private fun animateAddPlayer(
        targetView: AppCompatImageView,
        team: Array<Card?>,
        spotIndex: Int
    ) {
        Animations.animateAddPlayer(flip_view, targetView) {
            // OnStop
            restoreFlipViewsPosition()
            vm.onPlayerAddedAnimationEnd(targetView, team, spotIndex) { prepareViewsToPulse() }
        }
    }

    private fun animateAttack(targetView: AppCompatImageView) {
        removeAllOnClickListeners()
        Animations.stopAllPulsingCards()

        val victimOriginalX = targetView.x
        val victimOriginalY = targetView.y

        Animations.animateAttackPlayer(flip_view, targetView, vm.getScreenWidth()) {
            // OnStop
            targetView.x = victimOriginalX
            targetView.y = victimOriginalY
            restoreFlipViewsPosition()
            vm.onAttackedAnimationEnd(targetView) { prepareViewsToPulse() }
        }
    }

    private fun prepareAttackPlayer(victimTeam: Array<Card?>, spotIndex: Int, victimView: AppCompatImageView) {
        if (spotIndex == 5) {
            // Attacking goalie
            isJustShotAtGoalie = true

            if (vm.canAttack(victimTeam, spotIndex, victimView)) {
                removeAllOnClickListeners()
                Animations.stopAllPulsingCards()

                vm.notifyMessageAttackingGoalie()

                val isTargetGoalieBottom = !isTeamBottomTurn()
                val targetView = if (isTargetGoalieBottom) flip_btm_goalie else flip_top_goalie
                val targetViewFront = if (isTargetGoalieBottom) flip_btm_goalie_front else flip_top_goalie_front
                val targetViewBack = if (isTargetGoalieBottom) flip_btm_goalie_back else flip_top_goalie_back
                val targetTeam = if (isTargetGoalieBottom) teamBottom else teamTop

                ViewUtil.setImagesOnFlipView(
                    targetView,
                    targetViewFront,
                    targetViewBack,
                    null,
                    ViewUtil.getRotatedBitmap(requireContext(), vm.resIdOfCard(tempGoalieCard)),
                    isVertical = false
                )

                victimView.setImageResource(android.R.color.transparent)
                victimView.tag = Integer.valueOf(android.R.color.transparent)

                Animations.animateScoredAtGoalie(
                    fading_view,
                    flip_view,
                    targetView,
                    vm.getScreenWidth(),
                    card_top_center.x,
                    tempGoalieCard,
                    { message -> updateMessageBox(message) },
                    {
                        // OnStop
                        onGoalieActionEnd(targetView, true, targetTeam)
                        updateScores()
                        addGoalieView(bottom = isTargetGoalieBottom, doNotFlip = true, doRemoveCardFromDeck = true)
                    }
                )
            }
        } else {
            // Attacking another player
            if (vm.canAttack(victimTeam, spotIndex, victimView))
                animateAttack(victimView)
        }
    }

    private fun prepareGoalieSaved(victimView: AppCompatImageView) {
        removeAllOnClickListeners()
        Animations.stopAllPulsingCards()

        isJustShotAtGoalie = true

        vm.notifyMessageAttackingGoalie()

        val isTargetGoalieBottom = !isTeamBottomTurn()
        val targetView = if (isTargetGoalieBottom) flip_btm_goalie else flip_top_goalie
        val targetViewFront = if (isTargetGoalieBottom) flip_btm_goalie_front else flip_top_goalie_front
        val targetViewBack = if (isTargetGoalieBottom) flip_btm_goalie_back else flip_top_goalie_back
        val targetTeam = if (isTargetGoalieBottom) teamBottom else teamTop

        ViewUtil.setImagesOnFlipView(
            targetView,
            targetViewFront,
            targetViewBack,
            null,
            ViewUtil.getRotatedBitmap(requireContext(), vm.resIdOfCard(tempGoalieCard)),
            isVertical = false
        )

        victimView.setImageResource(android.R.color.transparent)
        victimView.tag = Integer.valueOf(android.R.color.transparent)

        Animations.animateGoalieSaved(
            fading_view,
            flip_view,
            targetView,
            vm.getScreenWidth(),
            card_top_center.x,
            tempGoalieCard,
            { message -> updateMessageBox(message) },
            {
                // OnStop
                onGoalieActionEnd(targetView, false, targetTeam)
                addGoalieView(bottom = isTargetGoalieBottom, doNotFlip = true, doRemoveCardFromDeck = true)
            }
        )
    }

    /*
    * On Animation Ends
    * */

    private fun onFlipPlayingCardEnd(isBadCard: Boolean) {
        flip_view.flipTheView()

        // Bot's turn
        if (isBotMoving && !isBadCard) {
            // Adding player
            if (isRestoringPlayers) {
                vm.botChooseEmptySpot(getEmptySpots()) {
                    // Trigger the bot's move
                    if (it != -1) animateAddPlayer(teamTopViews[it], teamTop, it)
                }
                // Attacking player
            } else {
                val chosenIndex = vm.botChooseIndexToAttack(possibleMovesIndexes)
                Log.d(TAG_GAMEACTIVITY, chosenIndex.toString())

                when (chosenIndex) {
                    -1 -> { /* Do nothing */
                    }
                    5 -> {
                        tempGoalieCard = teamBottom[5]
                        if (vm.canAttack(teamBottom, 5, card_bm_goalie)) prepareAttackPlayer(
                            teamBottom,
                            5,
                            card_bm_goalie
                        )
                        else prepareGoalieSaved(card_bm_goalie)
                    }
                    else -> prepareAttackPlayer(teamBottom, chosenIndex, teamBottomViews[chosenIndex])
                }
            }
        } else {
            if (!isBadCard) {
                setOnClickListeners()
            } else {
                // If it is bad card, this runs
                Animations.animateBadCard(
                    flip_view,
                    vm.getScreenWidth(),
                    { removeAllOnClickListeners() },
                    {
                        // OnStop
                        vm.notifyToggleTurn()
                        restoreFlipViewsPosition()
                        vm.removeCardFromDeck()

                        if (!vm.isThisTeamReady()) {
                            updateMessageBox("Please choose a position.")
                            isOngoingGame = false
                            isRestoringPlayers = true
                        }

                        if (isOngoingGame && !GameLogic.isTherePossibleMove(whoseTurn, vm.firstCardInDeck)) {
                            vm.triggerBadCard()
                        } else if (isOngoingGame && GameLogic.isTherePossibleMove(whoseTurn, vm.firstCardInDeck)) {
                            prepareViewsToPulse()
                        }
                    })
            }
        }

        if (isJustShotAtGoalie) isJustShotAtGoalie = false
    }

    private fun onGoalieActionEnd(view: View, isGoal: Boolean = false, team: Array<Card?>) {
        fading_view.visibility = View.GONE
        view.visibility = View.GONE
        isOngoingGame = false
        isRestoringPlayers = true

        if (isGoal) vm.addGoalToScore()

        team[5] = null
        vm.notifyToggleTurn()
        restoreFlipViewsPosition()
        updateMessageBox("Please choose a position.")
    }

    /*
    * Game management
    * */

    private fun isNextPeriodReady(nextPeriod: Int): Boolean {
        removeAllOnClickListeners()

        period += nextPeriod
        isRestoringPlayers = true

        cards_left.visibility = View.GONE
        restoreFlipViewsPosition()

        return if (vm.isNextPeriodReady()) {
            resetAllCards(teamBottomViews)
            resetAllCards(teamTopViews)
            card_top_goalie.tag = null
            card_bm_goalie.tag = null
            true
        } else {
            whole_view.visibility = View.VISIBLE
            false
        }
    }

    private fun getEmptySpots(): List<Int> {
        val list = mutableListOf<Int>()

        teamTopViews.minus(teamTopViews.last()).forEachIndexed { index, view ->
            if (view.tag == Integer.valueOf(android.R.color.transparent)) list.add(index)
        }

        Log.d(TAG_GAMEACTIVITY, "$list")

        return list
    }

    /*
    * OnClickListeners
    * */

    private fun setOnClickListeners() {
        teamBottomViews.forEach { it.setOnClickListener(this) }
        teamTopViews.forEach { it.setOnClickListener(this) }
    }

    private fun removeAllOnClickListeners() {
        teamBottomViews.forEach { it.setOnClickListener(null) }
        teamTopViews.forEach { it.setOnClickListener(null) }
    }

    override fun onClick(v: View) {
        if (isBotMoving) return

        val spotIndex: Int
        if (isOngoingGame) {
            if (v.tag == Integer.valueOf(android.R.color.transparent)) return
            if (isTeamBottomTurn()) {
                spotIndex = when (v.id) {
                    R.id.card_top_forward_left -> 0
                    R.id.card_top_center -> 1
                    R.id.card_top_forward_right -> 2
                    R.id.card_top_defender_left -> if (vm.areEnoughForwardsOut(teamTop, 3)) 3 else return
                    R.id.card_top_defender_right -> if (vm.areEnoughForwardsOut(teamTop, 4)) 4 else return
                    R.id.card_top_goalie -> if (vm.isAtLeastOneDefenderOut(teamTop)) 5 else return
                    else -> return
                }
            } else {
                spotIndex = when (v.id) {
                    R.id.card_bm_forward_left -> 0
                    R.id.card_bm_center -> 1
                    R.id.card_bm_forward_right -> 2
                    R.id.card_bm_defender_left -> if (vm.areEnoughForwardsOut(teamBottom, 3)) 3 else return
                    R.id.card_bm_defender_right -> if (vm.areEnoughForwardsOut(teamBottom, 4)) 4 else return
                    R.id.card_bm_goalie -> if (vm.isAtLeastOneDefenderOut(teamBottom)) 5 else return
                    else -> return
                }
            }

            val imageView = view?.findViewById<AppCompatImageView>(v.id)
            val targetTeam = if (isTeamBottomTurn()) teamTop else teamBottom

            imageView?.let {
                if (spotIndex == 5) {
                    tempGoalieCard = targetTeam[5]
                    if (vm.canAttack(targetTeam, 5, it))
                        prepareAttackPlayer(targetTeam, 5, it)
                    else prepareGoalieSaved(it)
                } else {
                    prepareAttackPlayer(targetTeam, spotIndex, it)
                }
            }
        } else {
            if (isTeamBottomTurn()) {
                spotIndex = when (v.id) {
                    R.id.card_bm_forward_left -> 0
                    R.id.card_bm_center -> 1
                    R.id.card_bm_forward_right -> 2
                    R.id.card_bm_defender_left -> 3
                    R.id.card_bm_defender_right -> 4
                    else -> return
                }
            } else {
                spotIndex = when (v.id) {
                    R.id.card_top_forward_left -> 0
                    R.id.card_top_center -> 1
                    R.id.card_top_forward_right -> 2
                    R.id.card_top_defender_left -> 3
                    R.id.card_top_defender_right -> 4
                    else -> return
                }
            }

            val imageView = view?.findViewById<AppCompatImageView>(v.id)
            val team = if (isTeamBottomTurn()) teamBottom else teamTop

            imageView?.let {
                if (vm.canAddPlayerView(imageView, team, spotIndex) && v.tag != null) {
                    removeAllOnClickListeners()
                    animateAddPlayer(imageView, team, spotIndex)
                }
            }
        }
    }
}