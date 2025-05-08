package eu.dataspace.connector.tests;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.AccessKey;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.util.UUID;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.IAM;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

public class S3Extension implements BeforeAllCallback, AfterAllCallback {

    private final DockerImageName dockerImage = DockerImageName.parse("localstack/localstack:4.3.0");

    private final LocalStackContainer localstack = new LocalStackContainer(dockerImage).withServices(S3, IAM);

    @Override
    public void beforeAll(ExtensionContext context) {
        localstack.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        localstack.stop();
    }

    public URI getEndpoint() {
        return localstack.getEndpoint();
    }

    public S3Client getS3Client() {

        return S3Client.builder()
                .credentialsProvider(getCredentialsProvider())
                .region(Region.of("eu-central-1"))
                .endpointOverride(localstack.getEndpoint())
                .build();
    }

    public IamClient getIamClient() {
        return IamClient.builder()
                .credentialsProvider(getCredentialsProvider())
                .region(Region.AWS_GLOBAL)
                .endpointOverride(localstack.getEndpoint())
                .build();
    }

    private @NotNull StaticCredentialsProvider getCredentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials
                .create(localstack.getAccessKey(), localstack.getSecretKey()));
    }

    /**
     * Creates a bucket with access policies
     *
     * @param bucketName bucket name.
     * @return the access key of the user that can access the bucket.
     */
    public AccessKey createBucket(String bucketName) {
        var iamClient = getIamClient();

        var userName = UUID.randomUUID().toString();
        var user = iamClient.createUser(b -> b.userName(userName)).user();

        var s3Client = getS3Client();
        s3Client.createBucket(b -> b.bucket(bucketName));
        s3Client.putPublicAccessBlock(b -> b
                .bucket(bucketName)
                .publicAccessBlockConfiguration(p -> p
                        .blockPublicAcls(true)
                        .ignorePublicAcls(true)
                        .blockPublicPolicy(true)
                        .restrictPublicBuckets(true)
                ));
        s3Client.putBucketPolicy(b -> b.bucket(bucketName)
                .policy(canAccessBucketPolicy(bucketName, user.userId())));

        return iamClient.createAccessKey(b -> b.userName(user.userName())).accessKey();
    }

    public void uploadToBucket(String bucketName, String objectName, byte[] content) {
        getS3Client().putObject(b -> b.bucket(bucketName).key(objectName), RequestBody.fromBytes(content));
    }

    private String canAccessBucketPolicy(String bucketName, String userId) {
        return """
                {
                     "Version": "2012-10-17",
                     "Statement": [
                         {
                             "Effect": "Deny",
                             "Principal": "*",
                             "Action": "s3:*",
                             "Resource": [
                                 "arn:aws:s3:::%s",
                                 "arn:aws:s3:::%s/*"
                             ],
                             "Condition": {
                                 "StringNotEquals": {
                                     "aws:PrincipalARN": ["arn:aws:iam::111122223333:user/%s"]
                                 }
                             }
                         }
                     ]
                 }
                """.formatted(bucketName, bucketName, userId);
    }
}
