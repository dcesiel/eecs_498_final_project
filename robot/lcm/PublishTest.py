import lcm
from lcmtypes import gun_t

msg = gun_t.gun_t()
msg.timestamp = 0
msg.fire = True;

lc = lcm.LCM()
lc.publish("GUN", msg.encode())
