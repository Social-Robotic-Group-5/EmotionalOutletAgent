package furhatos.app.outletagentskill

import furhatos.app.outletagentskill.flow.Init
import furhatos.flow.kotlin.Flow
import furhatos.skills.Skill

class OutletagentskillSkill : Skill() {
    override fun start() {
        Flow().run(Init)
    }
}

fun main(args: Array<String>) {
    Skill.main(args)
}
