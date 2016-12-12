#!/bin/bash

SEBAL_CONTAINER_NAME="sebal_container"

print_menu() {
        echo "Usage: $0 COMMAND [OPTIONS]"
        echo "Commands are LIST and DOWNLOAD"
        echo "LIST | LIST -o inputs/outputs"
        echo "DOWNLOAD --image-name image-name --input-directory input-directory --output-directory output-directory"
        exit 1
}

define_parameters() {
        while [ ! -z $1 ]; do
                case $1 in
			-o | --option)
                                shift;
                                option=$1;
                                ;;
                        --input-directory)
                                shift;
                                input_directory=$1;
                                ;;
                        --output-directory)
                                shift;
                                output_directory=$1;
                                ;;
                        --image-name)
                                shift;
                                image_name=$1
                                ;;
                esac
                shift
        done
}

do_list() {
	define_parameters $@
	bash list-swift-container.sh $SEBAL_CONTAINER_NAME $option
}

do_download() {
	define_parameters $@
	bash swift-cli.sh $SEBAL_CONTAINER_NAME $image_name $input_directory $output_directory
}

if [ $# -gt 0 ]
then
    op=$1
    case "$op" in
        LIST)
            shift
            do_list $@
        ;;
        DOWNLOAD)
            shift
            do_download $@
        ;;
        *)
	    print_menu
            exit 1
        ;;
    esac
else
        print_menu
        exit 1
fi
