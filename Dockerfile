# Use an official Nginx image as the base image
FROM nginx:latest

# Set working directory in the container
WORKDIR /usr/share/nginx/html

# Copy the static website files from the extracted directory
COPY . .

# Expose port 80
EXPOSE 80

# Start Nginx server
CMD ["nginx", "-g", "daemon off;"]
