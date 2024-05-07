#!/bin/bash

url="http://demo-htb-one-id-453949642.ap-southeast-1.elb.amazonaws.com/api/users-test?total="

if [ -z "$1" ]; then
    total=10
else
    total="$1"
fi

url="$url$total"

# Directory to save the file
output_dir="./"
output_file="user_email.json"

# Check if the file exists
if [ ! -f "${output_dir}${output_file}" ]; then
    mkdir -p "$output_dir" # Create directory if it doesn't exist
fi

# Send the request and store the response in the 'response' variable
echo "Sending HTTP request to $url"
response=$(curl -s -X GET "$url")

# Check if there was an error in sending the HTTP request
if [ $? -eq 0 ]; then
    # Save the JSON result to the file user_email.json in the directory ../user-files/lib/
    echo "$response" > "${output_dir}${output_file}"
    echo "Result saved to ${output_dir}${output_file}"
else
    echo "Error sending HTTP request"
fi