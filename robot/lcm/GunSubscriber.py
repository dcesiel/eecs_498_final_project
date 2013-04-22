import lcm
from lcmtypes import gun_t
from pyfirmata import Arduino, util
import time

DEBUG = True

#Initialize Arduino board on ttyUSB0
board = Arduino('/dev/ttyUSB0')
print("GunSubscriber connected to Arduino")

#Initialize servo PWM
#This sets pin 10 to a digital pwm output
pin10 = board.get_pin('d:6:s')

#Initialize digital output to start gun
pin13 = board.get_pin('d:13:o')

def handler(channel, data):
    msg = gun_t.gun_t.decode(data)
    if DEBUG:
        print("   timestamp   = %s" % str(msg.timestamp))
        print("   fire = %s" % str(msg.fire))

    board.digital[13].write(1)
    time.sleep(0.2)
    pin10.write(175)
    time.sleep(1.3)
    pin10.write(23)
    time.sleep(0.8)
    board.digital[13].write(0)


lc = lcm.LCM()
subscription = lc.subscribe("GUN", handler)

try:
    while True:
        lc.handle()
        time.sleep(0.02);
except KeyboardInterrupt:
    pass
