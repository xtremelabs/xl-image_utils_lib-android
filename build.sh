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
	MANIFEST_FILE_NAME=xl-lib-manifest.xml
	for param in `cat $MANIFEST_FILE_NAME`
	do
		tag=`echo "$param" | sed "s/<\([^>]*\)>.*/\1/"`
		value=`echo "$param" | sed "s/^[^>]*>\([^<]*\).*/\1/"`
		case "$tag" in
			API_LEVEL) 	API_LEVEL=$value
						;;
			VERSION)	VERSION=$value
						;;
			GIT_PROJECT) GIT_PROJECT=$value
						;;
			JAR_FILE_NAME) JAR_FILE_PREFIX=$value
						;;
			PROJECT_NAME) PROJECT_NAME=$value
						;;
			PROJECT_DIR) PROJECT_DIR=$value
						;;
		esac
	done

	BUILD_TAG=v$VERSION
	JAR_FILE_NAME=$JAR_FILE_PREFIX-$BUILD_TAG.jar
	WORKSPACE=$(pwd)

	RESULTANT_APK_DIR=$WORKSPACE/$PROJECT_DIR
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
	TARGET_JAR_FILE=$PROJECT_DIR/bin/$JAR_FILE_NAME

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


}

# Tags build and uploads APKs to the server
function publish_build {

	print_header "Publishing builds"


	echo
	echo "Committing jar and manifest git to project"
	echo

	git add $MANIFEST_FILE_NAME
	git add $PROJECT_DIR/$JAR_FILE_NAME

	git commit -m "Updated ${JAR_FILE_NAME} and manifest to version ${VERSION}"

	git push origin master

	echo
	echo "Applying git tag to project"
	echo


	# Apply a git tag to the project
	git tag -a "$BUILD_TAG" -m "Tag for version ${VERSION}" || {
		print_error "git tag failed"
	}

	# Push tags to remote repo
	git push --tags || {
		print_error "git push --tags failed"
	}	

	echo
	echo "This build has been tagged with following tag in git and can be found here:"
	echo "  https://github.com/$GIT_PROJECT/tree/$BUILD_TAG"
}

# Tags build and uploads APKs to the server
function generate_manifest {
	print_header "Generating manifest"

	echo "<Manifest>" > $MANIFEST_FILE_NAME
	echo "	<API_LEVEL>$API_LEVEL</API_LEVEL>" >> $MANIFEST_FILE_NAME
	echo "	<VERSION>$VERSION</VERSION>" >> $MANIFEST_FILE_NAME
	echo "	<GIT_PROJECT>$GIT_PROJECT</GIT_PROJECT>" >> $MANIFEST_FILE_NAME
	echo "	<JAR_FILE_NAME>$JAR_FILE_PREFIX</JAR_FILE_NAME>" >> $MANIFEST_FILE_NAME
	echo "	<PROJECT_NAME>$PROJECT_NAME</PROJECT_NAME>" >> $MANIFEST_FILE_NAME
	echo "	<PROJECT_DIR>$PROJECT_DIR</PROJECT_DIR>" >> $MANIFEST_FILE_NAME
	echo "	<Library path=\"$PROJECT_DIR/$JAR_FILE_NAME\" />" >> $MANIFEST_FILE_NAME
	echo "</Manifest>">> $MANIFEST_FILE_NAME
}

set_vars
pre_build
generate_manifest 
cd $PROJECT_DIR
update_project $PROJECT_NAME
clean_project $PROJECT_NAME
set_as_library $PROJECT_NAME
build_library $PROJECT_NAME $PROJECT_DIR
cd ..
publish_build
