// vars/demo.groovy

def call(String instanceId, String region, String s3Bucket) {
    withAWS(credentials: 'aws-ssm-credentials', region: region) {
        echo "🚀 Executing SSM command on instance: ${instanceId} in region: ${region}"

        // Run AWS SSM Command to execute deployment script on EC2
        sh """
        aws ssm send-command --document-name "AWS-RunShellScript" --targets "Key=instanceids,Values=${instanceId}" --parameters 'commands=[
            "#!/bin/bash",
            "set -e",
            
            "echo \\"[INFO] Updating System...\\"",
            "sudo apt-get update -y",
            "sudo apt-get install -y unzip curl jq",

            "echo \\"[INFO] Checking for AWS CLI Installation...\\"",
            "if ! command -v aws &> /dev/null; then",
            "    echo \\"[INFO] Installing AWS CLI...\\"",
            "    sudo apt-get remove -y awscli",
            "    curl \"https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip\" -o \"awscliv2.zip\"",
            "    unzip awscliv2.zip",
            "    sudo ./aws/install",
            "    aws --version",
            "    rm -rf awscliv2.zip aws",
            "fi",

            "echo \\"[INFO] Downloading deployment package from S3...\\"",
            "mkdir -p /home/ubuntu/deploy",
            "aws s3 cp s3://\${s3Bucket}/deploy.zip /home/ubuntu/deploy/deploy.zip",
            "unzip -o /home/ubuntu/deploy/deploy.zip -d /home/ubuntu/deploy",

            "echo \\"[INFO] Checking for Docker installation...\\"",
            "if ! command -v docker &> /dev/null; then",
            "    echo \\"[INFO] Installing Docker...\\"",
            "    sudo apt-get update -y",
            "    sudo apt-get install -y docker.io",
            "    sudo systemctl enable docker",
            "    sudo systemctl start docker",
            "fi",

            "echo \\"[INFO] Cleaning up old Docker containers...\\"",
            "sudo docker ps -q | xargs -r sudo docker stop",
            "sudo docker ps -aq | xargs -r sudo docker rm",
            "sudo docker images -q | xargs -r sudo docker rmi -f",

            "echo \\"[INFO] Building and Running Docker Container...\\"",
            "cd /home/ubuntu/deploy",
            "if [ -f Dockerfile ]; then",
            "    export DOCKER_BUILDKIT=1",
            "    sudo docker build -t app-container .",
            "    sudo docker run -d -p 80:80 --name app-container app-container",
            "else",
            "    echo \\"[ERROR] Dockerfile not found! Exiting...\\"",
            "    exit 1",
            "fi",

            "echo \\"[INFO] Deployment Successful! Access your app on port 80.\\""
        ]' --region ${region}
        """
    }
}
