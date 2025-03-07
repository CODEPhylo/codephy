#!/bin/bash

# Set the base directory for BEAST
BEAST_DIR="/Applications/BEAST 2.7.5/lib/packages"

# Make sure the directory exists
if [ ! -d "$BEAST_DIR" ]; then
    echo "Error: BEAST directory not found at $BEAST_DIR"
    exit 1
fi

# Function to install a JAR file
install_jar() {
    local jarFile="$1"
    local artifactId="$2"
    local version="$3"
    
    # Extract just the filename without path
    local fileName=$(basename "$jarFile")
    
    echo "Installing $fileName to local Maven repository..."
    
    mvn install:install-file \
        -Dfile="$jarFile" \
        -DgroupId="beast2" \
        -DartifactId="$artifactId" \
        -Dversion="$version" \
        -Dpackaging="jar" \
        -DgeneratePom=true
        
    echo "Installation of $fileName completed."
}

# Install each JAR file
install_jar "$BEAST_DIR/BEAST.app.jar" "beast-app" "2.7.5"
install_jar "$BEAST_DIR/BEAST.base.jar" "beast-base" "2.7.5"
install_jar "$BEAST_DIR/BEAST.app.src.jar" "beast-app-src" "2.7.5"
install_jar "$BEAST_DIR/BEAST.base.src.jar" "beast-base-src" "2.7.5"

echo "All BEAST JAR files have been installed to your local Maven repository."
