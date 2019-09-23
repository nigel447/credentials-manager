package com.krypto.manager.controllers

import com.krypto.manager.AppArtifacts
import com.krypto.manager.AppStore
import com.krypto.manager.models.CipherWithParams
import com.krypto.manager.models.KeyStoreWrapper
import org.bouncycastle.util.encoders.Base64
import tornadofx.Controller
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


/**
 * CP === Cipher Parameters
 */

abstract class ControllerBase: Controller() {

    val salt = ByteArray(64)
    val CREDS_ACCESS_KEY = "access"
    val CREDS_ACCESS_SEC = "secret"

    val ACCOUNT_ID_KEY = "encryptedAccountId"
    val COGNITO_POOL_KEY = "encryptedPoolId"
    val COGNITO_CLIENT_ID_KEY = "encryptedClientId"

   fun getSecretKey( ): SecretKey {
        var entry: KeyStore.SecretKeyEntry? = null
        val keyStore = KeyStore.getInstance("BKS", "BC")
        val inStream = ByteArrayInputStream(Base64.decode(AppStore.KEY_STORE.get()))
        keyStore.load(inStream, AppStore.KEY_STORE_PASSWD.get().toCharArray());
        val keyPassProtection = KeyStore.PasswordProtection(AppStore.SYM_KEY_PSSWD.get().toCharArray())
        try {
            entry  = keyStore.getEntry(AppStore.ALIAS.get(), keyPassProtection) as KeyStore.SecretKeyEntry;
        } catch(e:Exception) {
            AppArtifacts.appLogger.error(e.localizedMessage)
        }
        return entry!!.secretKey
    }


   fun doEncrypt(key: SecretKey, data: ByteArray): CipherWithParams {
        val cipher = Cipher.getInstance("AES/CFB/NoPadding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return CipherWithParams(cipher.getIV(), cipher.doFinal(data))
    }
}

class KeyStoreGeneratorController: ControllerBase()  {


    fun generateKeyStoreWithParaphrase( ) {
        AppStore.KEY_STORE.set( wrapSKeyInJKSAsB64String( AppStore.KEY_STORE_PASSWD.get().toCharArray(), AppStore.SYM_KEY_PSSWD.get().toCharArray(), genSecretKey(AppStore.SYM_KEY_PSSWD.get().toCharArray()) ) )
        AppStore.KEY_STORE_JSON_WRAP.set( AppArtifacts.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(  KeyStoreWrapper(String(AppStore.SYM_KEY_PSSWD.get().toCharArray() ),
                                                                                String(AppStore.KEY_STORE_PASSWD.get().toCharArray() ), AppStore.KEY_STORE.get() )) )
    }

    private fun genSecretKey(password: CharArray): SecretKey {
        val defaultRandom = SecureRandom.getInstance("SHA1PRNG")
        defaultRandom.nextBytes(salt);
        val generator = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        val hmacKey = generator.generateSecret(PBEKeySpec(password, salt, 1024, 256));
        return SecretKeySpec(hmacKey.getEncoded(), "AES");
    }

    private fun wrapSKeyInJKSAsB64String(storePass: CharArray, keyPass: CharArray, secretKey: SecretKey): String {

        val keyStore = KeyStore.getInstance("BKS", "BC");
        keyStore.load(null, null);
        // chain parameter is null normally certs
        keyStore.setKeyEntry(AppStore.ALIAS.get(), secretKey, keyPass, null);

        val storeAsBytes = ByteArrayOutputStream()
        keyStore.store(storeAsBytes, storePass);

        val b64EncodedBytes  = Base64.encode(storeAsBytes.toByteArray())
        return String(b64EncodedBytes, Charset.forName("UTF-8"))

    }

}

class CredentialsGeneratorController: ControllerBase()  {

    fun processAWSKeysEncryption( ): Boolean {

        val key = getSecretKey( )
        val aResultCP = doEncrypt(key, AppStore.ACCESS.get().toByteArray())
        val sResultCP = doEncrypt(key,AppStore.SECRET.get().toByteArray())
        val dataMap = mapOf(CREDS_ACCESS_KEY to  AppArtifacts.objectMapper.writeValueAsString(aResultCP), CREDS_ACCESS_SEC to  AppArtifacts.objectMapper.writeValueAsString(sResultCP))
        AppStore.ENCRYPTED_CREDENTIALS_JSON_WRAP.set( AppArtifacts.objectMapper.writeValueAsString(dataMap))
        return true

    }
}

class CognitoGeneratorController: ControllerBase()  {

    fun processCognitoArtifactsEncryption( ): Boolean {

        val key = getSecretKey( )
        val acctIDCP = doEncrypt(key, AppStore.ACCOUNT_ID.get().toByteArray())
        val poolResultCP  = doEncrypt(key,AppStore.COGNITO_POOL_ID.get().toByteArray())
        val cliIdResultCP = doEncrypt(key,AppStore.COGNITO_CLIENT_ID.get().toByteArray())
        val dataMap = mapOf(ACCOUNT_ID_KEY to  AppArtifacts.objectMapper.writeValueAsString(acctIDCP),
                                COGNITO_POOL_KEY to  AppArtifacts.objectMapper.writeValueAsString(poolResultCP), COGNITO_CLIENT_ID_KEY to  AppArtifacts.objectMapper.writeValueAsString(cliIdResultCP))

        AppStore.ENCRYPTED_COGNITO_JSON_WRAP.set( AppArtifacts.objectMapper.writeValueAsString(dataMap))
        return true

    }


}