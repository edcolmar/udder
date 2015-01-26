# build a json file with the studio config

pixel_spacing = 0.5
strip_gap = 5.0

initial_y_location = 0.0
initial_x_location = 0.0
initial_z_location = 0.0

current_y_location = initial_y_location
current_x_location = initial_x_location
current_z_location = initial_z_location

current_address = 0

build_list = []

# build something like this

#     --
#  |     |
#  |
#  |


#     64
#  30    64
#  64
#  30


#    125-189
# 95-125  189-253
# 31-95
# 1-30


for i in range(30):
    print i
    
    build_list.append({
        'address': current_address,
        'group': 0,
        'point': [
            current_x_location,
            current_y_location,
            current_z_location
        ]
    })
    current_y_location = current_y_location - pixel_spacing
    current_address = current_address + 1
    
current_y_location = current_y_location - strip_gap

for i in range(64):
    print i
    
    build_list.append({
        'address': current_address,
        'group': 0,
        'point': [
            current_x_location,
            current_y_location,
            current_z_location
        ]
    })
    current_y_location = current_y_location - pixel_spacing
    current_address = current_address + 1
    
current_y_location = current_y_location - strip_gap


for i in range(30):
    print i
    
    build_list.append({
        'address': current_address,
        'group': 0,
        'point': [
            current_x_location,
            current_y_location,
            current_z_location
        ]
    })
    current_y_location = current_y_location - pixel_spacing
    current_address = current_address + 1
    
current_y_location = current_y_location - strip_gap
current_x_location = current_x_location + strip_gap

for i in range(64):
    print i
    
    build_list.append({
        'address': current_address,
        'group': 0,
        'point': [
            current_x_location,
            current_y_location,
            current_z_location
        ]
    })
    current_x_location = current_x_location + pixel_spacing
    current_address = current_address + 1
    
current_y_location = current_y_location + strip_gap
current_x_location = current_x_location + strip_gap

for i in range(64):
    print i
    
    build_list.append({
        'address': current_address,
        'group': 0,
        'point': [
            current_x_location,
            current_y_location,
            current_z_location
        ]
    })
    current_y_location = current_y_location + pixel_spacing
    current_address = current_address + 1
    


print(build_list)
    
    