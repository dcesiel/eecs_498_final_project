import lcm
from lcmtypes import drive_t
from pyfirmata import Arduino, util

DEBUG = False

def handler(channel, data):
    msg = drive_t.decode(data)
    if DEBUG:
        print("   timestamp   = %s" % str(msg.timestamp))
        print("   front_motor = %s" % str(msg.front_motor))
        print("   back_motor = %s" % str(msg.back_motor))
        print("   right_motor = %s" % str(msg.right_motor))
        print("   left_motor = %s" % str(msg.left_motor))



lc = lcm.LCM()
subscription = lc.subscribe("MOTOR", handler)

board = Arduino('/dev/ttyUSB0')

#Initialize PWMS

try:
    while True:
        lc.handle()
except KeyboardInterrupt:
    pass
