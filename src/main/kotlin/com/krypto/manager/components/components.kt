package com.krypto.manager.components

import com.krypto.manager.AppStore
import com.krypto.manager.ManagerStyle
import com.krypto.manager.ResetEvent
import javafx.application.Platform
import javafx.geometry.VPos
import javafx.scene.control.Button
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import tornadofx.*


class Header : View() {

    override val root = borderpane() {
        left(textflow {
            text("Krypto Manager") {
                font = Font.font("monospaced", FontWeight.THIN, FontPosture.ITALIC, 30.0)
                fill = Color.WHITE
                strokeWidth = 1.0
                stroke = Color.GOLDENROD
            }
        })


        right(hbox(20) {
            add(button("Reset") {
                HBox.setMargin(this, insets(4, 10, 0, 20))
                addClass(ManagerStyle.appHeaderButton)
                action {
                    fire(ResetEvent())
                }

            })

            add(button("Quit") {
                HBox.setMargin(this, insets(4, 10, 0, 20))
                addClass(ManagerStyle.appHeaderButton)
                action {
                  Platform.exit()
                }
            })
        }
        )

            addClass(ManagerStyle.appHeader)
            HBox.setHgrow(this, Priority.ALWAYS)
    }


}