#!/bin/bash

# Prints an error during a build that requires cleanup and then exits the script
function print_build_error {
	echo
	echo "ERROR: $1"
	exit 1
}

function print_header {
	echo
	echo "#################### $1 ####################" 
	echo
}

function set_vars {
	API_LEVEL=15
	VERSION=1.0
	TIMESTAMP=$(date "+%Y%m%d%H%M")-$BUILD_TAG
	GIT_PROJECT=xtremelabs/xl-image_utils_lib-android
	# KEYSTORE_FILE=$WORKSPACE/wirelessgen.keystore
	# KEYSTORE_ALIAS=androiddebugkey
	# KEYSTORE_PASSWORD_FILE=~/keystores/wirelessgen.keystore.pwd
	# DEPLOY_HOST=ubuntu@assets.xtremelabs.com
	# DEPLOY_DIR=/opt/mobile/wirelessgen-android
	# DEPLOY_KEY=~/.ssh/assets-keypair.pem
	# DEPLOY_URL_BASE=http://assets.xtremelabs.com/wirelessgen-android
	# BUILD_REPORT=$WORKSPACE/build-report.txt
	 RESULTANT_APK_DIR=$WORKSPACE/$PROJECT_DIR
	# DOWNLOAD_DIR=$WORKSPACE/bin/download

	# RESULTANT_DEBUG_URLS=()
	# RESULTANT_RELEASE_URLS=()
	# RESULTANT_MAPPING_FILE_URLS=()
	# RESULTANT_UPLOADABLE_FILES=()
}

# for_each_project function that runs 'android update project' on the given project
function update_project {

	PROJECT_NAME=$1

	print_header "Updating project '$PROJECT_NAME'"

	android update project --path . --target "android-$API_LEVEL" || {
		print_error "ERROR: Error updating project settings for project '$PROJECT_NAME'"
	}

	echo 'Done!'

	return 0
}

# for_each_project function that runs 'ant clean' on the given project
function clean_project {

	PROJECT_NAME=$1

	print_header "Cleaning project '$PROJECT_NAME'"

	ant clean || {
		echo
		echo "WARNING: Error cleaning project '$PROJECT_NAME'"
		return 1
	}

	echo 'Done!'

	return 0
}

# for_each_project function that sets the given project to be an Android library
function set_as_library {

	# Project related settings.
	PROJECT_NAME=$1

	print_header "Setting project '$PROJECT_NAME' as library"

	# Changes the project.properties file to make this project a library
	sed -e 's/android\.library=false/android.library=true/' project.properties > project.properties.sed || {
		print_error "Set for project.properties (setting android.library=true) failed"
	}
	mv project.properties.sed project.properties || {
		print_error "Could not move project.properties.sed over top of project.properties"
	}

	echo 'Done!'

	return 0
}

# for_each_project function that builds a library
function build_library {

	# Project related settings.
	PROJECT_NAME=$1
	PROJECT_DIR=$WORKSPACE/$2

	print_header "Hack building lib '$PROJECT_NAME' with timestamp '$TIMESTAMP'"

	SOURCE_JAR_FILE=$PROJECT_DIR/bin/classes.jar
	TARGET_JAR_FILE=$PROJECT_DIR/bin/${PROJECT_NAME}.jar

	export CLASSPATH=
	ant clean debug || {
		print_build_error "Debug build failed"
	}

	# Rename the JAR file to the expected name
	mv "$SOURCE_JAR_FILE" "$TARGET_JAR_FILE" || {
		print_build_error "Rename lib jar failed"
	}

	# Copy the resultant JAR to a common local directory
	cp "$TARGET_JAR_FILE" "$RESULTANT_APK_DIR" || {
		print_build_error "Could not copy '$TARGET_JAR_FILE' to directory '$RESULTANT_APK_DIR'"
	}
}

# Prebuild steps for debug and release builds
function pre_build {


	# Make a local directory to store resultant APKs
	if [ ! -d "$RESULTANT_APK_DIR" ]; then
		echo "Making directory '$RESULTANT_APK_DIR'"
		mkdir -p "$RESULTANT_APK_DIR" || {
			print_error "Could not create directory for resultant APKs '$RESULTANT_APK_DIR'"
		}
	fi

	## Copy the keystore file overtop of the developer's existing debug.keystore file
	#if [ "$IS_BUILDING" ]; then
	#	echo Copying keystore file over the existing debug.keystore file
	#	cp wirelessgen.keystore ~/.android/debug.keystore || {
	#		print_error "Could not copy keystore file!"
	#	}
	#fi
}

# Tags build and uploads APKs to the server
function publish_build {

	print_header "Publishing builds"

	echo
	echo "Applying git tag to project"
	echo

	# Apply a git tag to the project
	git tag -a "$TIMESTAMP" -m "Tag for week ${VERSION}" || {
		print_error "git tag failed"
	}

	# Push tags to remote repo
	git push --tags || {
		print_error "git push --tags failed"
	}

	echo
	echo "This build has been tagged with following tag in git and can be found here:"
	echo "  https://github.com/$GIT_PROJECT/tree/$TIMESTAMP"
}


# function set_build_tag {
# 	BUILD_TAG=
# 	# Don't accept an empty command line
# 	if [ $# -eq 0 ]; then
# 		print_usage
# 		exit 1
# 	fi

# 	if [ "$LAST_ARG" = "publish" ]; then
# 		BUILD_TAG=$ARG

# 					# Show error if user has a 'publish' argument and doesn't specify a tag
# 	if [ "$BUILD_PUBLISH" -a -z "$BUILD_TAG" ]; then
# 		print_usage_error "Must specify a tag with 'publish' argument"
# 	fi
# }




WORKSPACE=$(pwd)
# Project related settings.
PROJECT_NAME=XtremeImageUtils
PROJECT_DIR=xl_image_utils_lib

# echo $PROJECT_DIR
set_vars

pre_build

cd $PROJECT_DIR
update_project $PROJECT_NAME

clean_project $PROJECT_NAME

set_as_library $PROJECT_NAME

build_library $PROJECT_NAME $PROJECT_DIR

cd ..



	# #print_header "Hack building lib '$PROJECT_NAME' with timestamp '$TIMESTAMP'"

	# SOURCE_JAR_FILE=$PROJECT_DIR/bin/classes.jar
	# TARGET_JAR_FILE=$PROJECT_DIR/bin/${PROJECT_NAME}.jar

	# export CLASSPATH=
	# ant clean debug || {
	# 	print_build_error "Debug build failed"
	# }
