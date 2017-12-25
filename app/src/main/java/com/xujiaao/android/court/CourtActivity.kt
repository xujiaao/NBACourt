package com.xujiaao.android.court

import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import java.util.*


class CourtActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_court)
        initComponents()
    }

    private fun initComponents() {
        val court = findViewById<CourtView>(R.id.court_view).apply {
            this.adapter = CourtAdapter(this@CourtActivity)

            setOnCourtStateChangedListener(::updateToggle)
            updateToggle(this)
        }

        findViewById<View>(R.id.root).setOnClickListener {
            val expanded = !court.isExpanded
            court.setExpanded(expanded, true)

            if (expanded) {
                (court.adapter as CourtAdapter).setPlayers(listOf(
                        Player(Player.Team.AWAY, Player.Position.CENTER),
                        Player(Player.Team.AWAY, Player.Position.POWER_FORWARD),
                        Player(Player.Team.AWAY, Player.Position.SMALL_FORWARD),
                        Player(Player.Team.AWAY, Player.Position.SHOOTING_GUARD),
                        Player(Player.Team.AWAY, Player.Position.POINT_GUARD),
                        Player(Player.Team.HOME, Player.Position.CENTER),
                        Player(Player.Team.HOME, Player.Position.POWER_FORWARD),
                        Player(Player.Team.HOME, Player.Position.SMALL_FORWARD),
                        Player(Player.Team.HOME, Player.Position.SHOOTING_GUARD),
                        Player(Player.Team.HOME, Player.Position.POINT_GUARD)))
            }
        }
    }

    private fun updateToggle(court: CourtView) {
        val toggle: TextView = findViewById(R.id.toggle)
        if (court.isCourtAnimationRunning) {
            toggle.visibility = View.GONE
        } else {
            toggle.visibility = View.VISIBLE
            toggle.setText(if (court.isExpanded) R.string.action_collapse else R.string.action_expand)
        }
    }

    private class Player(val team: Team, val position: Position) {

        enum class Team {
            AWAY, HOME
        }

        enum class Position(val abbr: String) {
            CENTER("C"), POWER_FORWARD("PF"), SMALL_FORWARD("SF"), SHOOTING_GUARD("SG"), POINT_GUARD("PG")
        }

        companion object {
            const val VIEWPORT_WIDTH = 94
            const val VIEWPORT_HEIGHT = 50
        }

        var x: Int
        var y: Int

        init {
            val random = Random()
            this.x = random.nextInt(VIEWPORT_WIDTH)
            this.y = random.nextInt(VIEWPORT_HEIGHT)
        }

        constructor(team: Team, position: Position, x: Int, y: Int) : this(team, position) {
            this.x = x
            this.y = y
        }

        override fun toString(): String {
            return "${team.name} - ${position.name} ($x, $y)".format(this)
        }
    }

    private class CourtAdapter(context: Context) : BaseAdapter() {

        private val mInflater: LayoutInflater = LayoutInflater.from(context)

        var mPlayers: List<Player>? = null

        fun setPlayers(players: List<Player>) {
            mPlayers = players

            notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return mPlayers?.size ?: 0
        }

        override fun getItem(position: Int): Player {
            return mPlayers!![position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val holder: CourtViewHolder
            if (convertView == null) {
                holder = CourtViewHolder(mInflater.inflate(R.layout.item_player, parent, false))
                holder.itemView.tag = holder
            } else {
                holder = convertView.tag as CourtViewHolder
            }

            return holder.bindPlayer(getItem(position)).itemView
        }
    }

    private class CourtViewHolder(val itemView: View) : View.OnClickListener {

        val mPosition: TextView = itemView.findViewById(R.id.position)
        val mLayoutParams: CourtView.LayoutParams = itemView.layoutParams as CourtView.LayoutParams

        var mPlayer: Player? = null

        init {
            mLayoutParams.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            mLayoutParams.viewportLeft = 0F
            mLayoutParams.viewportTop = 0F
            mLayoutParams.viewportRight = Player.VIEWPORT_WIDTH.toFloat()
            mLayoutParams.viewportBottom = Player.VIEWPORT_HEIGHT.toFloat()

            itemView.setOnClickListener(this)
        }

        fun bindPlayer(player: Player): CourtViewHolder {
            mPlayer = player

            mPosition.text = player.position.abbr

            if (player.team == Player.Team.AWAY) {
                mPosition.setTextColor(ContextCompat.getColor(itemView.context, R.color.home_player))
            } else {
                mPosition.setTextColor(ContextCompat.getColor(itemView.context, R.color.away_player))
            }

            mLayoutParams.x = player.x.toFloat()
            mLayoutParams.y = player.y.toFloat()

            return this
        }

        override fun onClick(view: View) {
            Toast.makeText(view.context, mPlayer.toString(), Toast.LENGTH_SHORT).show()
        }
    }
}