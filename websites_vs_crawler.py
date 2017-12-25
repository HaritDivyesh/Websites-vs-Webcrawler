import os
import requests
from requests.adapters import HTTPAdapter
import pandas as pd
import time
from seleniumrequests import Chrome
from selenium.webdriver.chrome.options import Options

class websites_vs_crawler():

	def init(self):
		pass

	################SELENIUM################
	def selenium_headless(self, WINDOW_SIZE):
		chrome_options = Options()  
		chrome_options.add_argument("--headless")  
		chrome_options.add_argument("--window-size=%s" % WINDOW_SIZE)

		CHROME_PATH = '/Applications/Google Chrome.app' #Change 1
		CHROMEDRIVER_PATH = './chromedriver' #Change 2


		#Load the sites list
		SITES_LIST = pd.read_csv("Top_500_sites.csv")
		print ("Loaded the sites list file")
		SITES = []
		for index, row in SITES_LIST.iterrows():
			SITES.append(row['Site'])

		#Query the sites
		FAILED = 0
		print ("Querying...")
		for rank, site in enumerate(SITES):
			try:
				webdriver = Chrome(executable_path=CHROMEDRIVER_PATH, chrome_options=chrome_options)

				resp = webdriver.request('GET', 'https://'+str(site))
				print ("Quering site #" + str(rank+1)+" -> "+str(resp))
				if str(resp) == "<Response [403]>":
					print ("403 received for:"+site)
					FAILED += 1
					continue

			except Exception as ex:
		         print ("Exception occurred for " + str(site))
		         print ("And the exception is:" + str(ex))
		         FAILED += 1

		print ("Failed for:" + str(FAILED) + " out of 500")
		webdriver.close()


	###############!SELENIUM################

	def crawl_sites(self, USER_AGENT):
		#Query for sites list using my AWS credentials
		#ACCESS_KEY_ID = "my_access_id"
		#SECRET_ACCESS_KEY = "my_access_key"
		#os.system("javac TopSites.java")
		#os.system("java TopSites " + ACCESS_KEY_ID + " " + SECRET_ACCESS_KEY)

		#Load the sites list
		SITES_LIST = pd.read_csv("Top_500_sites.csv")
		print ("Loaded the sites list file")
		SITES = []
		for index, row in SITES_LIST.iterrows():
			SITES.append(row['Site'])

		#Query the sites
		FAILED = 0
		print ("Querying...")
		for rank, site in enumerate(SITES):

			try:
				s = requests.Session()
				s.mount('http://', HTTPAdapter(max_retries=5))
				resp = s.get("http://"+ site, headers={'User-Agent': USER_AGENT}, timeout = 5)
				print ("Quering site #" + str(rank+1)+" -> "+str(resp))
				if str(resp) != "<Response [200]>":
					print ("Something other than 200 for: "+site, "and the response is:"+str(resp))
					FAILED += 1
					continue

			except Exception as ex:
		         print ("Exception occurred for " + str(site))
		         print ("And the exception is:" + str(ex))
		         FAILED += 1

		print ("Failed for:" + str(FAILED) + " out of 500")

if __name__ == "__main__":
	crawler = websites_vs_crawler()
	#Empty user-agent
	crawler.crawl_sites("")
	#Custom user-agent
	crawler.crawl_sites("DivBot / https://sites.google.com/view/web-crawler-cs653")
	#Browser-like user-agent
	crawler.crawl_sites("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36")
	#Selenium
	WINDOW_SIZE = "2880,1800" #Change 3
	crawler.selenium_headless(WINDOW_SIZE)

