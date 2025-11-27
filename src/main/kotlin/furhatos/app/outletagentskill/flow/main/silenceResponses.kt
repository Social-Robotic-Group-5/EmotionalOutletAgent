package furhatos.app.outletagentskill.flow.main

import furhatos.flow.kotlin.utterance

val defaultSilenceResponse = utterance {
    random {
        +"Sorry, didn't hear you"
        +"Sorry, can't hear you"
        +"I'm sorry, can't hear you"
    }
    random {
        +"Perhaps you could speak louder"
        +"Maybe you can come closer"
        +"Maybe you could speak louder"
    }
}