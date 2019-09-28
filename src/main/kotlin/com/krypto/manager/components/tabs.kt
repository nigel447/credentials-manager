package com.krypto.manager.components

import com.amazonaws.services.s3.AmazonS3
import com.krypto.manager.*
import com.krypto.manager.controllers.CognitoGeneratorController
import com.krypto.manager.controllers.CredentialsGeneratorController
import com.krypto.manager.controllers.KeyStoreGeneratorController
import com.krypto.manager.controllers.STSCredentialsCheckController
import javafx.geometry.Orientation
import javafx.scene.control.Button
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import tornadofx.*

/**
 *  common expressions like spacings ect
 */
abstract class TabsBase : View() {
    val model = ViewModel()
    var output = text()

    val copyToClipboard = button("Copy to clipboard") {
        isVisible = false
        isManaged = false
        VBox.setMargin(this, insets(0, 0, 0, 10))
    }

    val outputContainer = hbox() {
        textflow {
            add(output)
            output.apply {
                isVisible = false
                isManaged = false
                addClass(ManagerStyle.outputText)
            }

        }
        VBox.setMargin(this, insets(0, 0, 0, 10))
        HBox.setHgrow(this, Priority.ALWAYS)
    }


    fun setClipboardContent(data: String) {
        val clipboard = Clipboard.getSystemClipboard()
        val content = ClipboardContent()
        content.putString(data)
        clipboard.setContent(content)
    }

    fun toggleOutPut() {
        output.isVisible = true
        output.isManaged = true
        copyToClipboard.isVisible = true
        copyToClipboard.isManaged = true
    }

    fun reset() {
        val clipboard = Clipboard.getSystemClipboard()
        val content = ClipboardContent()
        content.putString("data")
        clipboard.setContent(content)
        copyToClipboard.isVisible = false
        copyToClipboard.isManaged = false
        output.text = ""
        output.isVisible = false
        output.isManaged = false
        AppStore.KEY_STORE_JSON_WRAP.set("")
        AppStore.KEY_STORE.set("")
        AppStore.ENCRYPTED_CREDENTIALS_JSON_WRAP.set("")
    }
}


class KeyStoreGenerator : TabsBase() {
    // https://github.com/edvin/tornadofx-guide/blob/master/part1/11.%20Editing%20Models%20and%20Validation.md
    private val ALIAS = model.bind { AppStore.ALIAS }
    private val controller: KeyStoreGeneratorController by inject()

    override val root = vbox(20) {

        form {
            fieldset("Enter Paraphrase:", labelPosition = Orientation.HORIZONTAL) {
                val aliasLabel = field("Paraphrase") {
                    passwordfield(ALIAS).required()
                }
                aliasLabel.label.apply {
                    addClass(ManagerStyle.formLabel)
                }
            }

            add(button("Create") {
                addClass(ManagerStyle.submitField)
                enableWhen(model.valid)
                isDefaultButton = true
                // useMaxWidth = true

                action {
                    model.commit()
                    runAsync {
                        AppArtifacts.appLogger.info("KeyStoreGenerator runAsync with alias begin")
                        controller.generateKeyStoreWithParaphrase()

                    } ui {
                        AppArtifacts.appLogger.info("KeyStoreGenerator runAsync calls back ok")
                        output.isVisible = true
                        output.isManaged = true
                        output.text = AppStore.KEY_STORE_JSON_WRAP.get()
                        copyToClipboard.isVisible = true
                        copyToClipboard.isManaged = true

                    } fail {
                        model.rollback()
                        AppArtifacts.appLogger.info("KeyStoreGenerator runAsync fails with ${it.localizedMessage}")
                    }
                }
                VBox.setMargin(this, insets(20, 0, 0, 114))
            })
        }

        add(copyToClipboard)

        add(outputContainer)

        copyToClipboard.apply {
            action {
                setClipboardContent(AppStore.KEY_STORE_JSON_WRAP.get())
            }
        }

        subscribe<ResetEvent> { event ->
            AppArtifacts.appLogger.info("KeyStoreGenerator ResetEvent")
            reset()
            ALIAS.setValue("")
        }


    }


}

class CredentialsGenerator : TabsBase() {

    private val controller: CredentialsGeneratorController by inject()
    private val ACCESS = model.bind { AppStore.ACCESS }
    private val SECRET = model.bind { AppStore.SECRET }

    override val root = vbox(20) {

        add(form {
            fieldset("Enter AccessKey:", labelPosition = Orientation.VERTICAL) {
                passwordfield(ACCESS).required()
            }

            fieldset("Enter SecretKey:", labelPosition = Orientation.VERTICAL) {
                passwordfield(SECRET).required()
            }
            button("Process") {
                enableWhen(model.valid)
                isDefaultButton = true
                useMaxWidth = true
                addClass(ManagerStyle.submitField)
                action {
                    model.commit()
                    runAsync {
                        AppArtifacts.appLogger.info("CredentialsGenerator runAsync with ACCESS  ${AppStore.ACCESS.get()}")
                        controller.processAWSKeysEncryption()

                    } ui {
                        AppArtifacts.appLogger.info("CredentialsGenerator runAsync calls back with $it")
                        toggleOutPut()
                        output.text = AppStore.ENCRYPTED_CREDENTIALS_JSON_WRAP.get()
                    } fail {
                        model.rollback()
                        AppArtifacts.appLogger.info("KeyStoreGenerator runAsync fails with ${it.localizedMessage}")
                    }
                }
                VBox.setMargin(this, insets(20, 0, 0, 0))
            }
        })

        add(copyToClipboard)

        add(outputContainer)

        copyToClipboard.apply {
            action {
                setClipboardContent(AppStore.ENCRYPTED_CREDENTIALS_JSON_WRAP.get())
            }
        }

        subscribe<ResetEvent> { event ->
            AppArtifacts.appLogger.info("CredentialsGenerator ResetEvent")
            reset()
            ACCESS.setValue("")
            SECRET.setValue("")

        }


        this.visibleProperty().bind(AppStore.ALIAS.isNotEmpty)
    }
}

class CognitoGenerator : TabsBase() {
    private val ACCOUNT_ID = model.bind { AppStore.ACCOUNT_ID }
    private val COGNITO_POOL_ID = model.bind { AppStore.COGNITO_POOL_ID }
    private val COGNITO_CLIENT_ID = model.bind { AppStore.COGNITO_CLIENT_ID }

    private val controller: CognitoGeneratorController by inject()

    override val root = vbox(20) {

        form {
            fieldset("Enter Account ID:", labelPosition = Orientation.VERTICAL) {
                passwordfield(ACCOUNT_ID).required()
            }

            fieldset("Enter Cognito Pool ID:", labelPosition = Orientation.VERTICAL) {
                passwordfield(COGNITO_POOL_ID).required()
            }

            fieldset("Enter Cognito Client ID:", labelPosition = Orientation.VERTICAL) {
                passwordfield(COGNITO_CLIENT_ID).required()
            }

            button("Process") {
                enableWhen(model.valid)
                isDefaultButton = true
                useMaxWidth = true
                addClass(ManagerStyle.submitField)
                action {
                    model.commit()
                    runAsync {
                        AppArtifacts.appLogger.info("CredentialsGenerator runAsync with ACCESS  ${AppStore.ACCESS.get()}")
                        controller.processCognitoArtifactsEncryption()

                    } ui {
                        AppArtifacts.appLogger.info("CredentialsGenerator runAsync calls back with $it")
                        toggleOutPut()
                        output.text = AppStore.ENCRYPTED_COGNITO_JSON_WRAP.get()

                    } fail {
                        model.rollback()
                        AppArtifacts.appLogger.info("KeyStoreGenerator runAsync fails with ${it.localizedMessage}")
                    }
                }
                VBox.setMargin(this, insets(20, 0, 0, 0))
            }
        }

        add(copyToClipboard)

        add(outputContainer)

        copyToClipboard.apply {
            action {
                setClipboardContent(AppStore.ENCRYPTED_COGNITO_JSON_WRAP.get())
            }
        }

        subscribe<ResetEvent> { event ->
            AppArtifacts.appLogger.info("CredentialsGenerator ResetEvent")
            reset()
            ACCOUNT_ID.setValue("")
            COGNITO_POOL_ID.setValue("")
            COGNITO_CLIENT_ID.setValue("")
        }

        this.visibleProperty().bind(AppStore.ALIAS.isNotEmpty)
    }
}

class STSCredentialsCheck : TabsBase() {

    private val controller: STSCredentialsCheckController by inject()

    /**
     * for sts assume role check
     * not available on local stack
     */
    private lateinit var s3: AmazonS3

    private lateinit var createClient: Button
    private lateinit var runSTSTest: Button
    private lateinit var runSessionTest: Button

    private val encryptedAccess = model.bind { AppStore.ENCRYPTED_ACCESS }
    private val encryptedSecret = model.bind { AppStore.ENCRYPTED_SECRET }
    private val roleArn = model.bind { AppStore.ROLE_ARN }


    private lateinit var encryptedAccessFieldset: Fieldset
    private lateinit var encryptedSecretFieldset: Fieldset

    fun toggleCredInput() {
        encryptedAccessFieldset.isVisible = false
        encryptedSecretFieldset.isVisible = false
        encryptedAccessFieldset.isManaged = false
        encryptedSecretFieldset.isManaged = false
    }

    override val root = vbox(20) {


        add(hbox(15) {
            createClient = button("Create STS Session") {
                enableWhen(model.valid)
                action {
                    model.commit()
                    controller.procesEncryptedKeys()
                    toggleCredInput()
                    STSClientSession.initSTS(STSSessionClientProvider.stsSession())
                    toggleOutPut()
                    output.text = "Created STS Session OK"
                    runSTSTest .isVisible = true
                }
            }

            runSTSTest = button("Obtain STS Credentials ") {
                action {
                    AppStore.STS_TEST_JSON_WRAP.set( assumeRole())
                    output.text = assumeRole()
                    output.text = AppStore.STS_TEST_JSON_WRAP.get()
                    runSessionTest .isVisible = true
                }
                isVisible = false
            }
            runSessionTest = button("run assume role test ") {
                action {
                    if(AppArtifacts.IS_LOCALSTACK) {
                        output.text = "assume role test not available on local stack"
                    } else {
                        output.text = runOperationAsRole()
                    }

                }
                isVisible = false
            }
            VBox.setMargin(this, insets(10, 0, 0, 20))
        })

        add(form {
            encryptedAccessFieldset = fieldset("Enter Encrypted AccessKey:", labelPosition = Orientation.VERTICAL) {
                passwordfield(encryptedAccess).required()
            }
            encryptedSecretFieldset = fieldset("Enter Encrypted SecretKey:", labelPosition = Orientation.VERTICAL) {
                passwordfield(encryptedSecret).required()
            }

            fieldset("enter role arn for session", labelPosition = Orientation.HORIZONTAL) {

                textfield(roleArn).required()

            }

        })

        add(copyToClipboard)

        add(outputContainer)

        copyToClipboard.apply {
            action {
                setClipboardContent(AppStore.STS_TEST_JSON_WRAP.get())
            }
        }

    }


    fun assumeRole(): String {
        var sessionJson = ""
        val awsCreds = STSClientSession.assumeRole()!!.credentials
        if (awsCreds != null) {
            val session = STSClientSession.obtainSTSCredentialsForRole(awsCreds)
            sessionJson = session.writeToJsonString()
        }
        AppArtifacts.appLogger.info("getSession create s3 with ${awsCreds.accessKeyId} ${awsCreds.secretAccessKey}  ${awsCreds.sessionToken} ")

        if(!AppArtifacts.IS_LOCALSTACK) {
            s3 = STSClientSession.createS3Session(awsCreds)
        }
        return sessionJson
    }

    fun runOperationAsRole(): String {
        var sessionJson = ""
        S3TestSetup.initS3(s3)
        AppArtifacts.appLogger.info("runSession s3 ok ")
        sessionJson =  S3TestSetup.listBuckets()
        return sessionJson
    }



}