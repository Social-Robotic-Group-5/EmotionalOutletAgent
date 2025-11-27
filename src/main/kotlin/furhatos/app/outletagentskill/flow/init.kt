package furhatos.app.outletagentskill.flow

import furhatos.app.outletagentskill.flow.main.Idle
import furhatos.app.outletagentskill.flow.main.StartConversation
import furhatos.app.outletagentskill.setting.DISTANCE_TO_ENGAGE
import furhatos.app.outletagentskill.setting.MAX_NUMBER_OF_USERS
import furhatos.flow.kotlin.State
import furhatos.flow.kotlin.furhat
import furhatos.flow.kotlin.state
import furhatos.flow.kotlin.users
import furhatos.util.Gender
import furhatos.util.Language

val Init: State = state {
    init {
        /** Set our default interaction parameters */
        users.setSimpleEngagementPolicy(DISTANCE_TO_ENGAGE, MAX_NUMBER_OF_USERS)
        furhat.setVoice(language= Language.ENGLISH_US, gender= Gender.FEMALE)
    }

    onEntry {
        goto(Idle)
    }
}