#Ding JIn
#CS455 Fall 2015 PA1
#10/7/2015


import sys
import socket
import time

host = str(sys.argv[1])
port = int(sys.argv[2])
size = 1024

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((host, port))


def csp():
	print '\n----------Connetion Setup Phase----------\n'
	print 'Enter setup message:'
	msg = input_send_msg()

	print '\nSending setup message....\n'
	
	data_recv = recv_msg()
	print 'Data Received: ',data_recv
	
	if data_recv[:3] == '404':
		close()
	else:
		#store the setup message and process them into mp()
		msg = msg.split()
		m_type = msg[1]
		p_num = int(msg[2])
		m_size = int(msg[3])
		s_delay = int(msg[4])
		mp(m_type,p_num,m_size,s_delay)


def mp(m_type,p_num,m_size,s_delay):
	print '\n----------Measurement Phase----------\n'
	seq_num = 1

	load_part = 'a' * m_size   #fill the payload with 1byte python character to the message size in setup message

	sum_rtt = 0
	sum_tput = 0
	
	print 'Sending probes....( Num:',p_num,';','Size:',m_size,'bytes )\n'
	while seq_num <= p_num:
		msg = str('m'+' '+str(seq_num)+' '+load_part+'\n')
		
		s.send(msg)  #send probe
		sent_time = 1000.0*time.time()  #record sent time
		data_recv = recv_msg()  #receive echo probe
		recv_time = 1000.0*time.time()  #record received time
	
		if data_recv[:3] == '404':
			close()
		
		#simple measurement for each probe
		rtt = recv_time - sent_time	
		tput = (m_size/1024.0)/(rtt/1000.0) #ignore the header size
		sum_rtt += rtt
		sum_tput += tput

		print 'Probe Num',seq_num,':'
		print 'Round trip time:', rtt,'ms\n'
		#print 'Throughput:',tput,'KBps\n'

		seq_num += 1

	if m_type == 'rtt':
		mean_rtt = sum_rtt/p_num
		print 'Average round trip time:',mean_rtt,'ms\n'
		
	if m_type == 'tput':
		mean_tput = sum_tput/p_num
		print 'Average throughput:',mean_tput,'KBps\n'

	ctp()


def ctp():
	print '\n----------Connection Termination Phase----------\n'
	s.send('t\n')
	print 'Sending termination message...\n'
	
	data_recv = recv_msg()
	print 'Data Received:',data_recv
	
	close()

	
def recv_msg():
	datall = ''
	continue_recv = True
	while continue_recv:
		data = s.recv(size)
		datall += data
		if '\n' in data:  #'\n' indicate the end of message
			continue_recv =False
	return datall


def close():
	print 'Connection Terminated\n'
	s.close()


def input_send_msg():
	msg = sys.stdin.readline()
	if msg[-2]=='n':   #Input of setup message is '\n' free;
		           #with or without '\n' in the end of setup message are both fine
		msg = msg[:-3]+msg[-1:]
	s.send(msg)
	return msg


csp()
s.close()


