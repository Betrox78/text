from mysql.connector import (connection)
from mysql.connector import errorcode
from dotenv import load_dotenv
import requests
import os

load_dotenv()
path = os.path.dirname(os.path.realpath(__file__))
WRAPPER_URL = os.getenv("WRAPPER_URL")
MEDIA_URL = os.getenv("MEDIA_URL")
db_config = {
    'user': os.getenv("DB_USER"),
    'password': os.getenv("DB_PASSWORD"),
    'host': os.getenv("DB_HOST"),
    'database': os.getenv("DB_DATABASE"),
    'raise_on_warnings': True
}

QUERY_GET_INVOICES = "SELECT id, document_id FROM invoice WHERE document_id IS NOT NULL;"
QUERY_UPDATE_INVOICE = "UPDATE invoice SET media_document_pdf_name = %s, media_document_xml_name = %s WHERE id = %s;"

try:
    conn = connection.MySQLConnection(**db_config)
    cursor = conn.cursor(buffered=True)
    cursorUpdate = conn.cursor(buffered=True)
    cursor.execute(QUERY_GET_INVOICES)
    for (invoice_id, document_id) in cursor:
        pdf_name = ''
        xml_name = ''
        print("{}, Downloading document: {}".format(invoice_id, document_id))
        file_pdf = path + '/documents/ ' + document_id + '.pdf'
        file_xml = path + '/documents/ ' + document_id + '.xml'
        response = requests.get(WRAPPER_URL + document_id + '.pdf')

        with open(file_pdf, 'wb') as f:
            f.write(response.content)

        response = requests.get(WRAPPER_URL + document_id + '.xml')
        with open(file_xml, 'wb') as f:
            f.write(response.content)

        files = {'pdf': open(file_pdf, 'rb'), 'xml': open(file_xml, 'rb')}
        media_response = requests.post(MEDIA_URL, files=files)
        data_response = media_response.json()
        if len(data_response['data']) > 0:
            print("{}, Uploaded documents: {} to media".format(invoice_id, document_id))
            for invoice in data_response['data']:
                if invoice["filename"][-3:] == 'pdf':
                    pdf_name = invoice["filename"]
                elif invoice["filename"][-3:] == 'xml':
                    xml_name = invoice["filename"]
            cursorUpdate.execute(QUERY_UPDATE_INVOICE, (pdf_name, xml_name, invoice_id))
        else:
            print("{}, Operation failed uploading document: {} to media".format(invoice_id, document_id))

    conn.commit()
    cursor.close()
    cursorUpdate.close()
    conn.close()
except connection.errors.Error as err:
    if err.errno == errorcode.ER_ACCESS_DENIED_ERROR:
        print("Something is wrong with your user name or password")
    elif err.errno == errorcode.ER_BAD_DB_ERROR:
        print("Database does not exist")
    else:
        print(err)
else:
    conn.rollback()
    cursor.close()
    cursorUpdate.close()
    conn.close()
