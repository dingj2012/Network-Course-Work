#Ding Jin
#CS455 Fall2015 PA1
#10/7/2015
#Modified from TA's script

import sys
import socket

host = str(sys.argv[1])
port = int(sys.argv[2])          
size = 1024
backlog =1

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((host, port))

s.send('Hello World')
print '\nSending data to server(',host,',port#:',port,')....\n'
data = s.recv(size)
s.close()

print 'Data from server:',repr(data)
