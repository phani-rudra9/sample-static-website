// vars/demo.groovy

def call(String instanceId, String region, String s3Bucket) {
    echo "🚀 Executing SSM command on instance: ${instanceId} in region: ${region}"

    if (!s3Bucket?.trim()) {
        error "❌ S3_BUCKET is empty or undefined! Please check the Jenkinsfile."
    }

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
        "    curl -o awscliv2.zip https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip",
        "    unzip awscliv2.zip",
        "    sudo ./aws/install",
        "    aws --version",
        "    rm -rf awscliv2.zip aws",
        "fi",
    
        "echo \\"[INFO] Downloading deployment package from S3 (Bucket: \\${s3Bucket})...\\"",
        "mkdir -p /home/ubuntu/deploy",
        "aws s3 cp s3://${s3Bucket}/demo.zip /home/ubuntu/deploy/demo.zip",
        "unzip -o /home/ubuntu/deploy/demo.zip -d /home/ubuntu/demo",
    
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
        "cd /home/ubuntu/demo",
        "if [ -f Dockerfile ]; then",
        "    export DOCKER_BUILDKIT=1",
        "    sudo docker build -t app-container .",
        
        "    NONE_IMAGE_ID=\\$(sudo docker images --filter \\"dangling=true\\" --format \\"{{.ID}}\\" | head -n 1 || echo \\"\\" )",
        "    if [ -n \\\"\\\${NONE_IMAGE_ID}\\\" ]; then",
        "        echo \\"Tagging dangling image: \${NONE_IMAGE_ID}\\"",
        "        sudo docker tag \${NONE_IMAGE_ID} demo:latest",
        "    else",
        "        echo \\"No untagged images found.\\"",
        "    fi",
        
        "    sudo docker run -d -p 80:80 --name app-container app-container",
        "else",
        "    echo \\"[ERROR] Dockerfile not found! Exiting...\\"",
        "    exit 1",
        "fi",
    
        "echo \\"[INFO] Deployment Successful! Access your app on port 80.\\""
    ]' --region ${region}
    """
}
