#Ding Jin(dingj@bu.edu)
#CS455 Fall2015 PA1
#10/7/2015

import sys
import socket
import time
import math

host = ''                 
port = int(sys.argv[1])
size = 1024 
backlog = 1

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.bind((host, port))
s.listen(backlog)

print '\nRunning serve( Port Num:',port,')....'
print 'Receiving data from client....\n' 

continue_c = True  #determine if the server should keep receiving
s_delay = 0  #server delay
count_seq_num = 1  #Track the probe sequence number
m_size = 0  #byte size of each probe


def csp(data,client):
	global s_delay,m_size
	print '\n-----CSP-----\n'
	
	#parse message into list and check if every element is valid
	data = data.split()   
		
	print 'Receving setup message....\n'
	
	if len(data)!=5:
		client.send('404 ERROR: Invalid Connection Setup Message\n')
		close(client)
	elif (data[1]!='rtt') and (data[1]!='tput'):
		client.send('404 ERROR: Invalid Connection Setup Message\n')
		close(client)
	elif (isinstance(data[2],int)) or (data[2]<=0):
		client.send('404 ERROR: Invalid Connection Setup Message\n')
		close(client)
	elif (isinstance(data[3],int)) or (data[3]<=0):
		client.send('404 ERROR: Invalid Connection Setup Message\n')
		close(client)
	elif (isinstance(data[4],int)) or (data[4]<0):
		client.send('404 ERROR: Invalid Connection Setup Message\n')
		close(client)
	else:
		m_size = int(data[3])
		s_delay = int(data[4])
		print 'Connection established\n'
		client.send('200 OK: Ready\n')

def mp(data,client):
	global count_seq_num,s_delay,m_size
	print '\n-----MP-----'
	print 'Recveing Probe No.',count_seq_num
	msg = data
	
	#parse message into list and check
	data = data.split()
	
	if len(data) != 3:
		client.send('404 ERROR: Invalid Measurement Message\n')
		close(client)
	elif (isinstance(data[1],int)) or (int(data[1])!=count_seq_num):
		client.send('404 ERROR: Invalid Measurement Message\n')
		close(client)
	elif (len(data[2])!= m_size):
		client.send('404 ERROR: Invalid Measurement Message\n')
		close(client)
	else:
		count_seq_num += 1
			
		#server delay part
		if(s_delay>0):
			time.sleep(s_delay/1000.0)
		client.send(msg)

def ctp(data,client):
	print '\n-----CTP-----\n'

	#check the single character message
	if data[0] == 't':
		client.send('200 OK: Closing Connection\n')
	else:
		client.send('404 ERROR: Invalid Connetion Termination Message\n')

	close(client)


def close(client):
	global continue_c
	print 'Client Connection Terminated\n'
	continue_c = False  #change global variable to end the loop in server()
	client.close()
	s.close()


def recv_msg(client):
	datall = ''
	continue_recv = True
	while continue_recv:
		data = client.recv(size)
		datall += data
		if '\n' in data:   #'\n' indicate the end of message,stop reciving 
			continue_recv =False
	return datall

def server():
	global continue_c,s_delay,count_seq_num,m_size 

	(client, address) = s.accept()
	while continue_c:
		data = recv_msg(client)
		if data[0] == 's':
			csp(data,client)
		elif data[0] == 'm':
			mp(data,client)
		elif data[0] == 't':
			ctp(data,client)
		else:
			#processing the very first time message here
			client.send('404 ERROR: Invalid Connection Setup Message\n')
			close(client)
			break

server()
