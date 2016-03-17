#Ding Jin
#CS455 Fall2015 PA1
#10/7/2015
#Modified from TA's script

import sys
import socket

host = ''                 
port = int(sys.argv[1])
size = 1024 
backlog = 1

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.bind((host, port))
s.listen(backlog)

print '\nRunning server on csa2.bu.edu....'
print 'Receiving data from client....\n' 

(clientsocket, address) = s.accept()

while 1:
	
	data = clientsocket.recv(size)
	if data:
		print 'Data from client:',data
		clientsocket.send(data)
	else:
		break
		
clientsocket.close()

