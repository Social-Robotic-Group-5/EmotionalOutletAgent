package furhatos.app.outletagentskill.flow.main 

import furhatos.app.outletagentskill.flow.main.Greeting 
import furhatos.flow.kotlin.State
import furhatos.flow.kotlin.furhat
import furhatos.flow.kotlin.onUserEnter
import furhatos.flow.kotlin.state

val Idle: State = state {
    onEntry {
        // 当没有人被识别时，Furhat 停止看向任何人
        furhat.attendNobody()
    }

    onUserEnter {
        // 当有人进入视野，Furhat 转向它
        furhat.attend(it)
        // 并跳转到 Greeting 状态开始对话
        goto(Greeting)
    }

}