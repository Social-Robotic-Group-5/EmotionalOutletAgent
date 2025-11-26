package furhatos.app.outletagentskill.flow.main

import furhatos.flow.kotlin.utterance

val geminiFailResponse = utterance {
    random {
        +"Sorry, couldn't think right now"
        +"I have trouble processing right now"
    }
    +"Do you want to try again?"
}