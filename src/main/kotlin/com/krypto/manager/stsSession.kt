package com.krypto.manager

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicSessionCredentials
import com.amazonaws.auth.policy.*
import com.amazonaws.auth.policy.actions.S3Actions
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.Bucket
import com.amazonaws.services.securitytoken.AWSSecurityTokenService
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest
import com.amazonaws.services.securitytoken.model.AssumeRoleResult
import com.amazonaws.services.securitytoken.model.Credentials
import org.bouncycastle.asn1.x500.style.RFC4519Style
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.*

class Creds(val access: String, val secret: String) : AWSCredentials {

    override fun getAWSAccessKeyId(): String {
        return this.access
    }

    override fun getAWSSecretKey(): String {
        return this.secret
    }

    fun debug(): String {
        return "${this.access} ::: ${this.secret} "
    }
}

class STSCredentialsWrapper {
    lateinit var accessKeyId: String
    lateinit var  secretAccessKey: String
    lateinit var  sessionToken: String
    lateinit var expiration: Date

    fun fromAssuemRole(creds: Credentials): STSCredentialsWrapper {
        accessKeyId = creds.accessKeyId ?: ""
        secretAccessKey = creds.secretAccessKey ?: ""
        sessionToken = creds.sessionToken  ?: ""
        expiration = creds.expiration
        return this
    }


    fun writeToJsonString(): String {
        val out = ByteArrayOutputStream()
        AppArtifacts.objectMapper.writerWithDefaultPrettyPrinter().writeValue(out, this)
        return String(out.toByteArray(), Charset.forName("UTF-8"))
    }

    override fun toString(): String {
        return "accessKeyId:$accessKeyId,secretAccessKey:$secretAccessKey,sessionToken$sessionToken,expiration:$expiration"
    }
}

object STSSessionClientProvider : AWSCredentialsProvider {

    val creds = Creds(AppStore.DECRYPTED_ACCESS.get(), AppStore.DECRYPTED_SECRET.get())

    fun s3Session(): AmazonS3 {
        val builder = AmazonS3ClientBuilder.standard()
            .withCredentials(this)
            .withPathStyleAccessEnabled(true)

        if (AppArtifacts.IS_LOCALSTACK) {
            builder.withEndpointConfiguration(endpointConfiguration(AppArtifacts.LOCALSTACK_STS_ENDPOINT))
        } else {
            builder.withRegion(Regions.US_EAST_2)
        }
        return builder.build()
    }

    fun stsSession(): AWSSecurityTokenService {
        AppArtifacts.appLogger.info("create AWSSecurityTokenService  creds ${creds.debug()} ")
        val builder = AWSSecurityTokenServiceClientBuilder.standard()
            .withCredentials(this)

        if (AppArtifacts.IS_LOCALSTACK) {
            builder.withEndpointConfiguration(endpointConfiguration(AppArtifacts.LOCALSTACK_STS_ENDPOINT))
        } else {
            builder.withRegion(Regions.US_EAST_2)
        }
        return builder.build()
    }
    private fun endpointConfiguration(endpointURL: String): AwsClientBuilder.EndpointConfiguration {
        return AwsClientBuilder.EndpointConfiguration(endpointURL, Regions.US_EAST_2.name);
    }

    override fun getCredentials(): AWSCredentials {
        return creds
    }

    override fun refresh() {

    }
}



object STSClientSession {
    lateinit var sts: AWSSecurityTokenService

    fun initSTS(stsTemp: AWSSecurityTokenService) {

        sts = stsTemp
        AppArtifacts.appLogger.info("initSTS init AWSSecurityTokenService ok ${sts.sessionToken} exists")
    }


    /**
     *  2 ways to get the session token
     *  AssumeRoleRequest role needed for assume
     *  easy confuse with GetSessionTokenRequest
     */
    fun assumeRole(): AssumeRoleResult? {
        AppArtifacts.appLogger.info("getSessionToken")
        val assumeReq = AssumeRoleRequest()
        assumeReq.roleArn = AppStore.ROLE_ARN.get()
        assumeReq.roleSessionName = AppArtifacts.STS_SESSION_TEST_NAME
        return   sts.assumeRole(assumeReq)
    }

    fun obtainSTSCredentialsForRole(creds: Credentials): STSCredentialsWrapper {
        return STSCredentialsWrapper().fromAssuemRole(creds)
    }

    // client from sts credentials not implemented in localstack
    fun createS3Session(creds: Credentials): AmazonS3 {
        val sessionCredentialsWrapper =  STSCredentialsWrapper().fromAssuemRole(creds)
        val sessionCreds = BasicSessionCredentials(sessionCredentialsWrapper.accessKeyId, sessionCredentialsWrapper.secretAccessKey,
                                                                                                    sessionCredentialsWrapper.sessionToken)

        return AmazonS3ClientBuilder.standard()
            .withCredentials(AWSStaticCredentialsProvider(sessionCreds))
            .withRegion(Regions.US_EAST_2).build()
    }

}


/**
 *  role only has list bucket permissions
 */
object S3TestSetup {

    lateinit var s3: AmazonS3

    fun initS3(s3Temp: AmazonS3) {
        s3 = s3Temp
    }

    fun listBuckets( ): String  {
        val buckets = mutableListOf<String>()
        AppArtifacts.appLogger.info("${s3.s3AccountOwner}")
        s3.listBuckets().forEach {
            buckets.add(it.name)
        }
        return buckets.joinToString()
    }

}
