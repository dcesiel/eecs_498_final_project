import lcm
from lcmtypes import drive_t
from pyfirmata import Arduino, util

DEBUG = False

#Initialize Arduino board on ttyUSB0
board = Arduino('/dev/ttyUSB0')
print("MotorSubscriber connected to Arduino")

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
    msg = drive_t.decode(data)
    if DEBUG:
        print("   timestamp   = %s" % str(msg.timestamp))
        print("   front_motor = %s" % str(msg.front_motor))
        print("   back_motor = %s" % str(msg.back_motor))
        print("   right_motor = %s" % str(msg.right_motor))
        print("   left_motor = %s" % str(msg.left_motor))

    #Left Motor Logic
    if msg.left_motor >= 0:
        pin3 = msg.left_motor
        pin2 = 0
    else:
        pin3 = -msg.left_motor
        pin2 = 1

    #Right Motor Logic
    if msg.right_motor >= 0:
        pin5 = msg.right_motor
        pin4 = 0
    else:
        pin5 = -msg.right_motor
        pin4 = 1

    #Back Motor Logic
    if msg.back_motor >= 0:
        pin9 = msg.back_motor
        pin8 = 0
    else:
        pin9 = -msg.back_motor
        pin8 = 1

    #Front Motor Logic
    if msg.front_motor >= 0:
        pin11 = msg.front_motor
        pin12 = 0
    else:
        pin11 = -msg.front_motor
        pin12 = 1


lc = lcm.LCM()
subscription = lc.subscribe("MOTOR", handler)

try:
    while True:
        lc.handle()
        time.sleep(0.02);
except KeyboardInterrupt:
    pass
