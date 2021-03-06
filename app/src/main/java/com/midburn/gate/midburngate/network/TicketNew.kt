package com.midburn.gate.midburngate.network

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class TicketNew(val ticket: InnerTicket, val gate_status: State) : Serializable {
    enum class State {
        @SerializedName("early_arrival")
        EARLY_ENTRANCE,
        @SerializedName("regular")
        MIDBURN
    }
}

data class InnerTicket(val barcode: String, val ticket_number: Int, val order_id: Int, val holder_name: String,
                       val type: String, val inside_event: EventEntry, val israeli_id: String,
                       val disabled_parking: Int, val entrance_group_id: Int,
                       val production_early_arrival: Boolean, val groups: Array<Group>) : Serializable {

    enum class EventEntry {
        /**
         * The ticket holder is inside the event
         */
        @SerializedName("1")
        INSIDE,
        /**
         * The ticket holder is outside the event
         */
        @SerializedName("0")
        OUTSIDE
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InnerTicket

        if (barcode != other.barcode) return false
        if (ticket_number != other.ticket_number) return false
        if (order_id != other.order_id) return false
        if (holder_name != other.holder_name) return false
        if (type != other.type) return false
        if (inside_event != other.inside_event) return false
        if (israeli_id != other.israeli_id) return false
        if (disabled_parking != other.disabled_parking) return false
        if (entrance_group_id != other.entrance_group_id) return false
        if (production_early_arrival != other.production_early_arrival) return false
        if (!groups.contentEquals(other.groups)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = barcode.hashCode()
        result = 31 * result + ticket_number
        result = 31 * result + order_id
        result = 31 * result + holder_name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + inside_event.hashCode()
        result = 31 * result + israeli_id.hashCode()
        result = 31 * result + disabled_parking
        result = 31 * result + entrance_group_id
        result = 31 * result + production_early_arrival.hashCode()
        result = 31 * result + groups.contentHashCode()
        return result
    }

}

data class Group(val id: Int, val name: String, val type: String) : Serializable