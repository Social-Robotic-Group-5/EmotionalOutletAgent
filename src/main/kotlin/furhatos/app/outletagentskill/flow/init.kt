package furhatos.app.outletagentskill.flow

import furhatos.app.outletagentskill.flow.main.StartConversation
import furhatos.app.outletagentskill.setting.DISTANCE_TO_ENGAGE
import furhatos.app.outletagentskill.setting.MAX_NUMBER_OF_USERS
import furhatos.flow.kotlin.State
import furhatos.flow.kotlin.state
import furhatos.flow.kotlin.users

val Init: State = state {
    init {
        /** Set our default interaction parameters */
        users.setSimpleEngagementPolicy(DISTANCE_TO_ENGAGE, MAX_NUMBER_OF_USERS)
    }

    onEntry {
        goto(StartConversation)
    }
}