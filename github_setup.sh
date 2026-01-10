#!/bin/bash

# Initialize Git
echo "Initializing Git repository..."
git init

# Add all files
echo "Adding files..."
git add .

# Commit
echo "Committing files..."
git commit -m "Initial commit for Signal WearOS app"

# Rename branch to main
git branch -M main

# Ask for Repository URL
echo ""
echo "Please create a new empty repository on GitHub."
echo "Enter the repository URL (e.g., https://github.com/YourUsername/SignalWearOS.git):"
read REPO_URL

if [ -z "$REPO_URL" ]; then
  echo "Error: Repository URL cannot be empty."
  exit 1
fi

# Add remote and push
echo "Adding remote origin..."
git remote add origin "$REPO_URL"

echo "Pushing to GitHub..."
git push -u origin main

echo ""
echo "Done! If the push failed, check your credentials or repository URL."
