from flask import Flask, render_template, request
from scan_receipt import scan_receipt
import json

IMG_PATH = "tmp/"

app = Flask(__name__)

@app.route("/", methods=['GET','POST'])
def home():
	return render_template("upload.html")

@app.route('/scan', methods = ['GET','POST'])
def upload_file():
	if request.method == 'POST':
		f = request.files['file']
		img = IMG_PATH + f.filename
		f.save(img)
		items, _, text = scan_receipt(img)
		print(text)
		return(json.dumps(items))		

if __name__ == "__main__":
	app.run(host="0.0.0.0", debug=True)
