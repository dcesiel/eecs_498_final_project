from pyfirmata import Arduino, util
import time
board = Arduino('/dev/ttyUSB0')
print("Connected to Arduino")

pin10 = board.get_pin('d:6:s')

pin10.write(25)
board.digital[13].write(1)
time.sleep(1)
while True:
    #angle = raw_input("Enter Angle: ")
    pin10.write(175)
    time.sleep(1.3)
    pin10.write(23)
    time.sleep(0.8)
