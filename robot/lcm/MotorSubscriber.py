import lcm
import time
from lcmtypes import drive_t
from pyfirmata import Arduino, util

DEBUG = True

#Initialize Arduino board on ttyUSB0
board = Arduino('/dev/ttyUSB0')
print("MotorSubscriber connected to Arduino")

lock = 0

it = util.Iterator(board)
it.start()

#Initialize PWMS
#This sets pin 3 to a digital pwm output
pin3 = board.get_pin('d:3:p')
pin5 = board.get_pin('d:5:p')
pin9 = board.get_pin('d:9:p')
pin11 = board.get_pin('d:11:p')

#Initialize digital outputs that are paired with PWM outputs
pin2 = board.get_pin('d:2:o')
pin4 = board.get_pin('d:4:o')
pin8 = board.get_pin('d:8:o')
pin12 = board.get_pin('d:12:o')

def handler(channel, data):

    msg = drive_t.drive_t.decode(data)

    global lock

    if msg.lock == 1:
        lock = True
    elif msg.lock == 2:
        lock = False

    if DEBUG:
        print("   timestamp   = %s" % str(msg.timestamp))
        print("   front_motor = %s" % str(msg.front_motor))
        print("   back_motor = %s" % str(msg.back_motor))
        print("   right_motor = %s" % str(msg.right_motor))
        print("   left_motor = %s" % str(msg.left_motor))

    if (lock == 0) or ((lock > 0) and (msg.lock != 0)):
        #Left Motor Logic
        if msg.left_motor >= 0:
            pin3.write(msg.left_motor)
            pin2.write(0)
        else:
            print("Working")
            pin3.write(1+msg.left_motor)
            pin2.write(1)

        #Right Motor Logic
        if msg.right_motor >= 0:
            pin5.write(msg.right_motor)
            pin4.write(0)
        else:
            pin5.write(1+msg.right_motor)
            pin4.write(1)

        #Back Motor Logic
        if msg.back_motor >= 0:
            pin9.write(msg.back_motor)
            pin8.write(0)
        else:
            pin9.write(1+msg.back_motor)
            pin8.write(1)

        #Front Motor Logic
        if msg.front_motor >= 0:
            pin11.write(msg.front_motor)
            pin12.write(0)
        else:
            pin11.write(1+msg.front_motor)
            pin12.write(1)


lc = lcm.LCM()
subscription = lc.subscribe("MOTOR", handler)

try:
    while True:
        lc.handle()
        time.sleep(0.02);
except KeyboardInterrupt:
    pass
