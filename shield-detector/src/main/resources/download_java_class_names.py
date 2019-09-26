#!/usr/bin/env python3

#
# Requires BeautifulSoup and requests libraries
#

from bs4 import BeautifulSoup
import requests

url = 'https://docs.oracle.com/javase/8/docs/api/allclasses-noframe.html'
print("Downloading {}...".format(url))

r = requests.get(url)
soup = BeautifulSoup(r.text, 'html.parser')

java_class_count = 0

output_file_name = "all_java_classes.txt"
output_file = open(output_file_name, "w")

for elem in soup.find(class_="indexContainer").ul.find_all("li"):
    java_class = elem.a["href"]
    if java_class.endswith(".html"):
        java_class = java_class[:-5]
    java_class = java_class.replace(".", "$")
    java_class = java_class.replace("/", ".")
    output_file.write(java_class)
    output_file.write("\n")
    java_class_count += 1

print("Downloaded {} java classes, stored them in {}".format(java_class_count, output_file_name))

