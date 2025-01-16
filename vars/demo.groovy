// vars/deemo.groovy
def call(String instanceId, String region, String s3Bucket) {
    withCredentials([aws(credentialsId: 'aws-ssm-credentials', region: region)]) {
        echo "Executing SSM command on instance: ${instanceId} in region: ${region}"

        def ssmCommand = """
        aws ssm send-command --document-name "AWS-RunShellScript" --targets "Key=instanceids,Values=${instanceId}" --parameters 'commands=["#!/bin/bash",
            "echo \\"Starting installation of AWS CLI...\\"",
            "sudo rm -rf /usr/local/aws* && sudo rm -rf aws*",
            "if ! command -v aws &> /dev/null; then",
            "    echo \\"AWS CLI not found, installing...\\"",
            "    sudo apt-get install -y unzip",
            "    curl https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip -o awscliv2.zip",
            "    unzip awscliv2.zip",
            "    sudo ./aws/install --update",
            "    if command -v aws &> /dev/null; then",
            "        echo \\"AWS CLI installed successfully.\\"",
            "        sudo rm -rf awscliv2.zip aws/",
            "    else",
            "        echo \\"AWS CLI installation failed.\\"",
            "    fi",
            "else",
            "    echo \\"AWS CLI is already installed, skipping installation.\\"",
            "fi",
            "mkdir -p /home/ubuntu/deploy",
            "echo \\"Creating directory for deployment...\\"",
            "aws s3 cp s3://${s3Bucket}/deploy.zip /home/ubuntu/deploy.zip",
            "echo \\"Downloaded zip file from S3...\\"",
            "yes | unzip -o /home/ubuntu/deploy.zip -d /home/ubuntu/deploy",
            "export DEBIAN_FRONTEND=noninteractive",
            "if ! command -v docker &> /dev/null; then",
            "    sudo apt-get update -y",
            "    sudo apt-get install docker.io -y",
            "fi",
            "sudo docker ps -q | xargs -r sudo docker stop",
            "sudo docker ps -aq | xargs -r sudo docker rm",
            "sudo docker images -q | xargs -r sudo docker rmi -f",
            "cd /home/ubuntu/deploy/deploy",
            "if [ -f dockerfile ]; then",
            "    export DOCKER_BUILDKIT=1",
            "    sudo docker build -t demo .",
            "    NONE_IMAGE_ID=$(sudo docker images --filter \\"dangling=true\\" --format \\"{{.ID}}\\" | head -n 1)",
            "    if [ -n \\"$NONE_IMAGE_ID\\" ]; then",
            "        sudo docker tag \\"$NONE_IMAGE_ID\\" demo:latest",
            "    else",
            "        echo \\"No untagged images found.\\"",
            "    fi",
            "else",
            "    echo \\"Dockerfile not found in /home/ubuntu.\\"",
            "    exit 1",
            "fi"
        ]' \
        --region ${region}
        """

        sh ssmCommand
    }
}
