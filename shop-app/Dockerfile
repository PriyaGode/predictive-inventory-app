# Use Node image
FROM node:18-alpine

# Set working directory
WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm install

# Copy the rest of the frontend code
COPY . .

# Expose React dev server port
EXPOSE 3000

# Start React dev server
CMD ["npm", "start"]